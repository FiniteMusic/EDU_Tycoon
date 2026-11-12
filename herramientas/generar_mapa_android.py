#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Construye mapa de Android liviano sin modificar Mapa_General.tmx.
La versión Tiled conservadora se genera junto al runtime; no borra nada del editor.
"""
from pathlib import Path
import sys, re, base64, zlib, struct, copy
import xml.etree.ElementTree as ET

ALIASES = {
 'Computo':'escom_hitbox','ESCOM':'escom_hitbox','Computacion':'escom_hitbox',
 'Comida':'Mac_and_cheese','Restaurante':'cafeteria',
 'medicina':'Edificio1','Medicina':'Edificio1',
 'Consultorio':'Edificio2',
 'Arte_Diseño_Deporte':'Arte_diseno_deporte','Arte_diseño_deporte':'Arte_diseno_deporte',
 'Arte_diseno_deporte':'Arte_diseno_deporte',
}

def fichas(kt):
    text = kt.read_text(encoding='utf-8')
    matches=re.findall(r'"([^"]+)"\s+to\s+Propiedad\s*\((.*?)\n\s*\)',text,re.S)
    out={}
    for key,body in matches:
        p=re.search(r'texturePrefix\s*=\s*"([^"]+)"',body)
        if p:out[p.group(1).casefold()+'lvl1.png'.casefold()]=key
    if len(out)<10:raise ValueError('No se pudieron analizar texturePrefix de Propiedad.kt')
    return out

def cgid(g): return int(g)&0x1fffffff

def propiedades(obj):
    props=obj.find('properties')
    if props is None: props=ET.SubElement(obj,'properties')
    return props

def setprop(obj,name,value,kind='string'):
    props=propiedades(obj)
    p=next((p for p in props.findall('property') if p.get('name')==name),None)
    if p is None:p=ET.SubElement(props,'property')
    p.set('name',name)
    if kind!='string':p.set('type',kind)
    p.set('value',str(value).lower() if isinstance(value,bool) else str(value))

def tile_image_lookup(root, map_dir):
    gids={}
    for ts in root.findall('tileset'):
        first=int(ts.get('firstgid'))
        if ts.get('source'):
            external=map_dir / ts.get('source')
            e=ET.parse(external).getroot()
            prefix=external.parent
        else:e=ts;prefix=map_dir
        for tile in e.findall('tile'):
            im=tile.find('image')
            if im is not None:
                gids[first+int(tile.get('id'))]=prefix/im.get('source')
    return gids

def build(proj):
    proj=Path(proj)
    mdir=proj/'assets'/'Mapa'; tmx=mdir/'Mapa_General.tmx'; kt=proj/'core'/'src'/'main'/'kotlin'/'io'/'moviles'/'IPN_Tycoon'/'Propiedad.kt'
    need=[tmx,kt,mdir/'calles'/'calles.tsx']
    absent=[p for p in need if not p.is_file()]
    if absent:raise FileNotFoundError('Faltan: '+', '.join(map(str,absent)))
    root=ET.parse(tmx).getroot()
    original=copy.deepcopy(root)
    paths=tile_image_lookup(root,mdir)
    catalog=fichas(kt)
    edi=next((e for e in root.findall('objectgroup') if e.get('name')=='Edificios'),None)
    if edi is None:raise ValueError('No existe capa Edificios')
    marked=[]
    anclajes={}
    for render_index, ob in enumerate(edi.findall('object')):
        gid=ob.get('gid')
        if not gid:continue
        img=paths.get(cgid(gid))
        if img is None:continue
        prop_id=catalog.get(img.name.casefold())
        if prop_id:
            setprop(ob,'edificioComprable',True,'bool')
            setprop(ob,'propiedadId',prop_id)
            marked.append((ob.get('id'),img.name,prop_id))
            gid_num = int(gid)
            anclajes[prop_id] = {
                'x': ob.get('x'), 'y': ob.get('y'),
                'w': ob.get('width') or '0',
                'h': ob.get('height') or '0',
                'rotation': ob.get('rotation') or '0',
                'sort_y': ob.get('y') or '0',
                'sort_index': render_index,
                # TMX almacena volteos en los tres bits superiores de gid.
                'flip_x': bool(gid_num & 0x80000000),
                'flip_y': bool(gid_num & 0x40000000),
                'diagonal': bool(gid_num & 0x20000000)
            }
    # Completar puntos de origen con los puntos de nombre en la misma referencia isométrica.
    p_layer=next((e for e in root.findall('objectgroup') if e.get('name')=='Puntos_origen'),None)
    n_layer=next((e for e in root.findall('objectgroup') if e.get('name')=='Puntos_Nombres'),None)
    if p_layer is None or n_layer is None:raise ValueError('Falta Puntos_origen o Puntos_Nombres')
    exists=set(ALIASES.get((p.get('name') or ''),(p.get('name') or '')).casefold() for p in p_layer.findall('object'))
    prop_ids=set(catalog.values());maxid=max(int(x.get('id','0')) for g in root.findall('objectgroup') for x in g.findall('object'))
    added=[]
    for p in n_layer.findall('object'):
        name=p.get('name') or ''
        canonical=ALIASES.get(name,name)
        if canonical.casefold() not in exists and canonical.casefold() in {x.casefold() for x in prop_ids}:
            maxid += 1
            point_object = ET.SubElement(p_layer, 'object', {
                'id': str(maxid), 'name': canonical,
                'x': p.get('x'), 'y': p.get('y')
            })
            ET.SubElement(point_object, 'point')
            added.append(canonical); exists.add(canonical.casefold())
    root.set('nextobjectid',str(maxid+1))
    # Archivo de edición con todos los gráficos y nivel 1 intactos.
    tiled_out=mdir/'Mapa_Tiled_Compras_Corregidas.tmx'
    ET.ElementTree(root).write(tiled_out,encoding='utf-8',xml_declaration=True)
    # El runtime NO debe precargar los sprites bloqueados.
    run_root=copy.deepcopy(root)
    # EDU_EDITOR_NIVELES_NO_RUNTIME: capas editor solo en TMX fuente.
    # Si permanecen, la poda del runtime cargaria decenas de PNG de niveles.
    for capa in list(run_root.findall('objectgroup')):
        if (capa.get('name') or '').startswith('EDU_NIVEL_'):
            run_root.remove(capa)
    runtime_buildings=next(e for e in run_root.findall('objectgroup') if e.get('name')=='Edificios')
    removed=0
    for obj in list(runtime_buildings.findall('object')):
        ps=obj.find('properties')
        if ps is not None and any(q.get('name')=='edificioComprable' and q.get('value')=='true' for q in ps.findall('property')):
            runtime_buildings.remove(obj);removed+=1
    # Sustituir SOLO las losetas base grasss.png / grass1.png por el césped rápido.
    # ¡NO eliminar la capa entera!: ahora puede contener pisos y detalles nuevos.
    # Las celdas no-grasss quedan en una capa dispersa y usan el render habitual.
    grass = next((e for e in run_root.findall('layer') if e.get('name') == 'Fondo_pasto'), None)
    floors_preserved = 0
    base_removed = 0
    if grass is not None:
        d = grass.find('data')
        if d is None or d.get('encoding') != 'base64':
            raise ValueError('Fondo_pasto: codificacion no soportada; no se borro nada')
        encoded = ''.join((d.text or '').split())
        raw = base64.b64decode(encoded)
        compression = d.get('compression','')
        if compression == 'zlib': raw = zlib.decompress(raw)
        elif compression == 'gzip':
            import gzip
            raw = gzip.decompress(raw)
        elif compression: raise ValueError('Fondo_pasto: compresion no soportada')
        if len(raw) % 4: raise ValueError('Fondo_pasto: bytes corruptos')
        base_gid = {gid for gid,image in paths.items() if image.name.casefold() in {'grasss.png','grass1.png'}}
        if not base_gid:
            raise ValueError('No hay referencias de pasto base en el tileset; revisar el runtime')
        tiles = list(struct.unpack('<'+'I'*(len(raw)//4),raw))
        expected = int(grass.get('width',run_root.get('width','0'))) * int(grass.get('height',run_root.get('height','0')))
        if len(tiles) != expected: raise ValueError('Fondo_pasto: dimensiones no coinciden')
        for index,gid in enumerate(tiles):
            if cgid(gid) in base_gid:
                tiles[index] = 0
                base_removed += 1
            elif gid: floors_preserved += 1
        if floors_preserved == 0:
            run_root.remove(grass)
        else:
            d.set('encoding','base64')
            d.set('compression','zlib')
            d.text = base64.b64encode(zlib.compress(struct.pack('<'+'I'*len(tiles), *tiles),9)).decode('ascii')
    print('Piso/Fondo_pasto: césped base eliminado=',base_removed,'tiles nuevos conservados=',floors_preserved)

    # Cada edificio comprado se dibuja desde Kotlin con el mismo transform
    # original de Tiled. Una capa invisible de puntos conserva los parámetros
    # sin forzar la carga de sus texturas (evita recuperar el lag del APK).
    for group in list(run_root.findall('objectgroup')):
        if group.get('name') == 'Puntos_Render_Edificios':
            run_root.remove(group)
    next_layer = max([int(v.get('id','0')) for v in run_root
                      if v.tag in ('objectgroup','layer','imagelayer')] + [0]) + 1
    next_obj = max([int(o.get('id','0')) for o in run_root.findall('.//object')] + [0]) + 1
    anchors_group = ET.SubElement(run_root, 'objectgroup', {
        'id': str(next_layer), 'name': 'Puntos_Render_Edificios', 'visible':'0'
    })
    for prop_id, data in sorted(anclajes.items()):
        if data['diagonal']:
            print('ADVERTENCIA: el volteo diagonal de',prop_id,
                  'requiere verificación visual en Android')
        o=ET.SubElement(anchors_group, 'object', {
            'id':str(next_obj), 'name':prop_id,
            'x':str(data['x']), 'y':str(data['y'])
        })
        next_obj += 1
        ET.SubElement(o, 'point')
        setprop(o,'renderW',data['w'],'float')
        setprop(o,'renderH',data['h'],'float')
        setprop(o,'renderRotation',str(-float(data['rotation'])),'float')
        setprop(o,'renderFlipX',data['flip_x'],'bool')
        setprop(o,'renderFlipY',data['flip_y'],'bool')
        setprop(o,'renderSortY',data['sort_y'],'float')
        setprop(o,'renderSortIndex',data['sort_index'],'int')

    # Tiled por defecto usa draworder='topdown' dentro de cada objectgroup;
    # conservar Y e índice originales antes de que libGDX convierta coordenadas.
    for group in run_root.findall('objectgroup'):
        setprop(group, '_eduDrawOrder', group.get('draworder','topdown'))
        for idx, obj in enumerate(group.findall('object')):
            if obj.get('gid') is None: continue
            setprop(obj,'_eduSortY',obj.get('y','0'),'float')
            setprop(obj,'_eduSortIndex',idx,'int')

    run_root.set('nextlayerid',str(max(next_layer+1,int(run_root.get('nextlayerid','0')))))
    run_root.set('nextobjectid',str(max(next_obj,int(run_root.get('nextobjectid','0')))))

    # Identificar GIDs realmente usados por objetos y las capas de tiles.
    used=set()
    for o in run_root.findall('.//object[@gid]'):used.add(cgid(o.get('gid')))
    for layer in run_root.findall('layer'):
        d=layer.find('data')
        if d is None or d.get('encoding')!='base64':raise ValueError('Capa no soportada: '+layer.get('name',''))
        b=base64.b64decode(''.join(d.text.split()))
        if d.get('compression')=='zlib':b=zlib.decompress(b)
        elif d.get('compression')=='gzip':
            import gzip;b=gzip.decompress(b)
        elif d.get('compression'):raise ValueError('Compression no soportada')
        used.update(cgid(i[0]) for i in struct.iter_unpack('<I',b) if i[0])
    # Solo calles: el catálogo de edificios queda fuera del TMX del APK.
    counts=[]
    for t in list(run_root.findall('tileset')):
        first=int(t.get('firstgid'))
        src=t.get('source')
        if src:
            src_root=ET.parse(mdir/src).getroot()
            ids={first+int(x.get('id')) for x in src_root.findall('tile')}
        else:
            ids={first+int(x.get('id')) for x in t.findall('tile')}
        present=used & ids
        if not present:
            run_root.remove(t);counts.append((src or t.get('name'),'quitado'))
        elif src:
            run_src=Path(src).with_name(Path(src).stem+'_runtime.tsx')
            rt_root=ET.parse(mdir/src).getroot()
            for tile in list(rt_root.findall('tile')):
                if first+int(tile.get('id')) not in used:rt_root.remove(tile)
            (mdir/run_src).parent.mkdir(parents=True,exist_ok=True)
            ET.ElementTree(rt_root).write(mdir/run_src,encoding='utf-8',xml_declaration=True)
            t.set('source',run_src.as_posix());counts.append((src,f'{len(rt_root.findall("tile"))} gráficos usados'))
        else:
            for tile in list(t.findall('tile')):
                if first+int(tile.get('id')) not in used:t.remove(tile)
    run_gids=tile_image_lookup(run_root,mdir)
    missing=used-set(run_gids)
    if missing:raise ValueError('GIDs usados sin imagen en runtime: '+repr(sorted(missing)))
    runtime=mdir/'Mapa_Android_Optimizado.tmx'
    ET.ElementTree(run_root).write(runtime,encoding='utf-8',xml_declaration=True)
    print('MAPA OPTIMIZADO GENERADO:',runtime)
    print('DISEÑO CONSERVADO:',tmx)
    print('COPIA EDITABLE CON IDENTIFICADORES:',tiled_out)
    print('Edificios comprables detectados:',len(marked))
    print('Anclajes visuales recuperados:',len(anclajes))
    print('Edificios removidos de texturas TMX en APK:',removed)
    print('Puntos de origen agregados:',added)
    print('Tilesets:',counts)
    print('GIDs de decoración/terreno usados:',len(used))
    # advertencia si no se reconocen todos los 18 edificios (cambios de nombres)
    if len(marked)<len(prop_ids):print('ADVERTENCIA: solo se marcaron',len(marked),'de',len(prop_ids),'propiedades (comprobar PNG en Edificios).')

if __name__=='__main__':
 if len(sys.argv)!=2:raise SystemExit('USO: py herramientas\\generar_mapa_android.py "D:\\ruta\\EDU_Tycoon"')
 build(sys.argv[1])
