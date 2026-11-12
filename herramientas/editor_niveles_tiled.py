#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""EDU Tycoon: editor visual por nivel Tiled -> geometria runtime pequena.

Uso desde raiz repo:
  py herramientas/editor_niveles_tiled.py --preparar
  py herramientas/editor_niveles_tiled.py --sincronizar
  py herramientas/editor_niveles_tiled.py --verificar

--preparar agrega capas OCULTAS EDU_NIVEL_2..N al TMX fuente, 1 objeto por
edificio de cada nivel, con gid de sus propias texturas existentes en TSX.
--sincronizar regenera el runtime optimizado y exporta un archivo de TEXTO
con las coordenadas y tamano editados. Ningun objeto de edicion entra al APK.
"""
from __future__ import annotations
import argparse
import copy
from datetime import datetime
import math
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAPDIR = ROOT / 'assets/Mapa'
SOURCE = MAPDIR / 'Mapa_General.tmx'
RUNTIME = MAPDIR / 'Mapa_Android_Optimizado.tmx'
DATA = MAPDIR / 'ajustes_niveles_runtime.txt'
GENERATOR = ROOT / 'herramientas/generar_mapa_android.py'
PROPERTY = ROOT / 'core/src/main/kotlin/io/moviles/IPN_Tycoon/Propiedad.kt'
PREFIX = 'EDU_NIVEL_'
FLAGS = 0xE0000000
MASK = 0x1FFFFFFF


def parse_catalog():
    text = PROPERTY.read_text(encoding='utf-8')
    records = re.findall(r'"([^"]+)"\s+to\s+Propiedad\s*\((.*?)\n\s*\)', text, re.S)
    result = {}
    for ident, body in records:
        tex = re.search(r'texturePrefix\s*=\s*"([^"]+)"',body)
        maxi = re.search(r'mejoraMax\s*=\s*(\d+)',body)
        if tex and maxi:
            result[ident] = {'prefijo':tex.group(1), 'max':int(maxi.group(1))}
    if len(result) < 16:
        raise ValueError(f'Catalogo incompleto ({len(result)} edificios). No se modifica el mapa.')
    return result


def property_value(obj, name):
    ps = obj.find('properties')
    if ps is None:return None
    for p in ps.findall('property'):
        if p.get('name') == name:
            return p.get('value',p.text)
    return None


def property_set(obj, name, value, typ='string'):
    ps = obj.find('properties')
    if ps is None:ps = ET.SubElement(obj,'properties')
    p = next((t for t in ps.findall('property') if t.get('name')==name),None)
    if p is None:p = ET.SubElement(ps,'property')
    p.attrib.update({'name':name,'type':typ,'value':str(value).lower() if isinstance(value,bool) else str(value)})


def tiles_by_gid(root):
    found={}; by_name={}
    for ts in root.findall('tileset'):
        first=int(ts.get('firstgid'))
        if ts.get('source'):
            path = MAPDIR/ts.get('source')
            if not path.is_file():raise FileNotFoundError(f'Tileset ausente: {path}')
            source=ET.parse(path).getroot()
        else:source=ts
        for tile in source.findall('tile'):
            img=tile.find('image')
            if img is None:continue
            filename=Path(img.get('source','')).name
            if not filename:continue
            gid=first+int(tile.get('id'))
            found[gid]=filename
            by_name.setdefault(filename.casefold(),(gid,img))
    return found,by_name


def parse_source():
    if not SOURCE.is_file():raise FileNotFoundError(f'Falta mapa fuente: {SOURCE}')
    root=ET.parse(SOURCE).getroot()
    if root.get('orientation')!='isometric':
        raise ValueError('Este editor requiere mapa de Tiled isometrico.')
    return root


def original_buildings(root,cat,gidfiles):
    buildings=next((g for g in root.findall('objectgroup') if g.get('name')=='Edificios'),None)
    if buildings is None:raise ValueError('Falta capa Edificios en Mapa_General.tmx')
    by_src={data['prefijo'].casefold()+'lvl1.png':key for key,data in cat.items()}
    result={}
    for ob in buildings.findall('object'):
        if not ob.get('gid'):continue
        prop = property_value(ob,'propiedadId')
        if prop not in cat:
            filename=gidfiles.get(int(ob.get('gid')) & MASK,'')
            prop=by_src.get(filename.casefold())
        if prop not in cat:continue
        if prop in result:
            raise ValueError(f'Dos objetos de nivel 1 para {prop} en Edificios')
        result[prop]=ob
    if len(result) < 16:
        missing=sorted(set(cat)-set(result))
        raise ValueError(f'Solo {len(result)} edificios con objeto nivel 1. Faltan: {missing}. Revisa TMX/TSX.')
    return result


def initial_geometry(id,level,base, tex_image):
    width=float(base.get('width','0'));height=float(base.get('height','0'))
    if width <=0 or height<=0:raise ValueError(f'Dimensiones invalidas para {id}')
    if id=='Biblioteca':factor={2:.87,3:.78}.get(level,.74)
    else:factor={2:.94,3:.88}.get(level,.84)
    if id=='Direccion' and level>=3:
        maxw=width*.72;maxh=height*.82
    elif id=='Biblioteca' and level==3:
        maxw=width*.70;maxh=height*1.04
    else:
        maxw=width*factor;maxh=height*factor
    if (id=='Direccion' and level>=3) or (id=='Biblioteca' and level==3):
        aspect=float(tex_image.get('width','0'))/max(1.,float(tex_image.get('height','0')))
        if aspect>0:
            maxw=min(maxw,maxh*aspect);maxh=maxw/aspect
    shift = width*.065 if id=='Direccion' and level>=3 else 0.
    # TmxMapLoader invierte el eje Y de los objetos de Tiled al cargar Android.
    # deltaWorldX = deltaX_tiled - deltaY_tiled; desplazamiento horizontal
    # de +shift = Tiled X +shift/2 y Tiled Y -shift/2.
    x=float(base.get('x','0'))+shift*.5
    y=float(base.get('y','0'))-shift*.5
    return x,y,maxw,maxh


def preparar():
    root=parse_source()
    catalog=parse_catalog()
    gidfile,filenames=tiles_by_gid(root)
    originals=original_buildings(root,catalog,gidfile)
    generated={int(g.get('name')[len(PREFIX):]):g for g in root.findall('objectgroup')
               if (g.get('name') or '').startswith(PREFIX)
               and (g.get('name') or '')[len(PREFIX):].isdigit()}
    levels=range(2,max(d['max'] for d in catalog.values())+1)
    nextlayer=max([int(a.get('id','0')) for a in root if a.tag in ('layer','objectgroup','imagelayer')]+[0])+1
    nextobj=max([int(a.get('id','0')) for a in root.findall('.//object')]+[0])+1
    created=0
    for level in levels:
        group=generated.get(level)
        if group is None:
            group=ET.SubElement(root,'objectgroup',{
                'id':str(nextlayer),'name':f'{PREFIX}{level}',
                'visible':'0','opacity':'0.78','color':'#ffd166','draworder':'topdown'
            })
            # La capa editable debe quedar debajo de Arboles_arbustos en Tiled
            # para que el mobiliario/decoracion siga tapando la fachada.
            arboles=next((node for node in root if node.tag=='objectgroup' and node.get('name')=='Arboles_arbustos'),None)
            if arboles is not None:
                root.remove(group)
                root.insert(list(root).index(arboles), group)
            nextlayer+=1
        existing=set()
        for obj in group.findall('object'):
            key=property_value(obj,'propiedadId')
            if key is not None:existing.add(key)
        for ident,rec in sorted(catalog.items()):
            if level>rec['max'] or ident in existing:continue
            original=originals.get(ident)
            if original is None:continue
            target=f"{rec['prefijo']}lvl{level}.png".casefold()
            found=filenames.get(target)
            if not found:
                raise ValueError(f'Sin PNG registrado en tileset para {ident} nivel {level}: {target}')
            gid,img=found
            x,y,w,h=initial_geometry(ident,level,original,img)
            orig_gid=int(original.get('gid'))
            new_gid=gid | (orig_gid & FLAGS)
            attrib={
                'id':str(nextobj),'name':f'{ident} NIVEL {level}',
                'gid':str(new_gid),'x':f'{x:.5f}','y':f'{y:.5f}',
                'width':f'{w:.4f}','height':f'{h:.4f}'
            }
            if original.get('rotation'):
                attrib['rotation']=original.get('rotation')
            obj=ET.SubElement(group,'object',attrib)
            property_set(obj,'propiedadId',ident)
            property_set(obj,'nivelEdificio',level,'int')
            property_set(obj,'editorSolo',True,'bool')
            nextobj+=1;created+=1
    root.set('nextlayerid',str(max(nextlayer,int(root.get('nextlayerid','0')))))
    root.set('nextobjectid',str(max(nextobj,int(root.get('nextobjectid','0')))))
    if created:
        backup=SOURCE.with_name(SOURCE.name+'.bak_editor_niveles_'+datetime.now().strftime('%Y%m%d_%H%M%S'))
        shutil.copy2(SOURCE,backup)
        tmp=SOURCE.with_suffix('.editor_tmp')
        try:
            ET.ElementTree(root).write(tmp,encoding='utf-8',xml_declaration=True)
            ET.parse(tmp)
            tmp.replace(SOURCE)
        except Exception:
            tmp.unlink(missing_ok=True)
            raise
        print(f'Capas visuales listas, {created} objetos creados. Fuente preservada en {backup.name}')
    else:
        print('Capas de nivel ya existentes. No se sobreescribieron sus ajustes.')
    for level in levels:
        print(f'  {PREFIX}{level}:',len(generated.get(level, next((g for g in root.findall('objectgroup') if g.get('name')==f'{PREFIX}{level}'), ET.Element('objectgroup'))).findall('object')),'objetos (oculta por defecto)')
    return created


def serialize_changes():
    root=parse_source()
    catalog=parse_catalog()
    # EDU_GEOMETRIA_RELATIVA_V3: anclaje estable del nivel 1.
    # NO exportar x+y de los sprites de Tiled como coordenada absoluta: el
    # mapa Android tiene sus propios puntos de renderizado y conversiones.
    gidfiles, _ = tiles_by_gid(root)
    originales = original_buildings(root, catalog, gidfiles)
    groups=[g for g in root.findall('objectgroup') if (g.get('name') or '').startswith(PREFIX)]
    if not groups:raise ValueError('No hay capas de editor. Ejecuta --preparar primero.')
    rows={}
    for g in groups:
        # No soportar offsets de capa, que cambiarian posiciones sin actualizar objetos.
        if abs(float(g.get('offsetx','0')))>1e-4 or abs(float(g.get('offsety','0')))>1e-4:
            raise ValueError('El offset de la capa '+g.get('name')+' no debe cambiar; mueve el objeto dentro de ella.')
        for obj in g.findall('object'):
            ident=property_value(obj,'propiedadId')
            lv=property_value(obj,'nivelEdificio')
            if ident not in catalog:raise ValueError('ID de edificio desconocido: '+str(ident))
            if lv is None or not lv.isdigit():raise ValueError('Nivel inválido: '+str(lv))
            level=int(lv)
            if level<2 or level>catalog[ident]['max']:
                raise ValueError(f'Nivel no permitido: {ident} N{level}')
            if g.get('name')!=f'{PREFIX}{level}':
                raise ValueError(f'{ident} N{level} está en capa equivocada {g.get("name")}')
            if (ident,level) in rows:
                raise ValueError(f'Objeto duplicado {ident} N{level}')
            if not obj.get('gid'):
                raise ValueError(f'{ident} N{level} ya no es objeto de imagen')
            gid=int(obj.get('gid'))
            if gid & 0x20000000:
                raise ValueError(f'{ident} N{level}: volteo diagonal de Tiled no soportado; usa rotación normal')
            x=float(obj.get('x'));y=float(obj.get('y'))
            width=float(obj.get('width'));height=float(obj.get('height'))
            rotation=float(obj.get('rotation','0'))
            vals=[x,y,width,height,rotation]
            if not all(math.isfinite(v) for v in vals) or width<=0 or height<=0:
                raise ValueError(f'{ident} N{level}: dimensiones/coordenadas inválidas')
            # Se exporta SOLAMENTE el movimiento respecto al objeto de nivel 1.
            # Así el runtime siempre se ancla en Puntos_Render_Edificios, como V7.
            base = originales.get(ident)
            if base is None:raise ValueError(f'Sin anclaje original de {ident}')
            dx_tiled=x-float(base.get('x'))
            dy_tiled=y-float(base.get('y'))
            # IMPORTANTE: libGDX TmxMapLoader transforma el eje Y del TMX
            # (yUp = mapHeight - y). Debemos invertir el delta Y antes
            # de proyectarlo con la misma fórmula que GameScreen.
            dx_world=dx_tiled-dy_tiled
            dy_world=(-dy_tiled-dx_tiled)*.5
            # Fallar con un mensaje útil si accidentalmente el objeto de Tiled
            # se desplazó hasta el origen u otra zona muy lejana del campus.
            if abs(dx_world)>max(width*2.5,5000.) or abs(dy_world)>max(height*2.5,5000.):
                raise ValueError(f'{ident} N{level} muy lejos de su edificio base: '
                                 f'delta=({dx_world:.1f},{dy_world:.1f}). '
                                 'Recolócalo en Tiled o revisa su nivel 1.')
            flipx=bool(gid & 0x80000000);flipy=bool(gid & 0x40000000)
            rows[(ident,level)]=(dx_world,dy_world,width,height,-rotation,flipx,flipy)
    expected={(key,level) for key,item in catalog.items() for level in range(2,item['max']+1)}
    missing=expected-set(rows)
    if missing:raise ValueError('Objetos de edición faltantes: '+repr(sorted(missing)))
    return rows


def text_of(rows):
    lines=['# EDU_GEOMETRIA_RELATIVA_V3 id|nivel|deltaWorldX|deltaWorldY|width|height|rotation|flipX|flipY',
           '# Delta relativo al nivel 1 y a Puntos_Render_Edificios; no usar posiciones absolutas.']
    for (ident,level),row in sorted(rows.items()):
        dx,dy,w,h,r,fx,fy=row
        lines.append(f'{ident}|{level}|{dx:.5f}|{dy:.5f}|{w:.5f}|{h:.5f}|{r:.5f}|{str(fx).lower()}|{str(fy).lower()}')
    return '\n'.join(lines)+'\n'


def verify_runtime_clean():
    if not RUNTIME.is_file():raise FileNotFoundError('Mapa_Android_Optimizado.tmx ausente')
    root=ET.parse(RUNTIME).getroot()
    leaked=[g.get('name') for g in root.findall('objectgroup') if (g.get('name') or '').startswith(PREFIX)]
    if leaked:raise ValueError('Capas visuales llegaron a runtime: '+str(leaked)+'. No instales APK.')
    # El runtime base no debe contener comprado renderizado como objeto Tiled.
    return len(root.findall('objectgroup'))


def sincronizar(regenerate=True):
    rows=serialize_changes()
    if regenerate:
        if not GENERATOR.is_file():raise FileNotFoundError(f'Generador optimizado ausente: {GENERATOR}')
        program=GENERATOR.read_text(encoding='utf-8')
        if 'EDU_EDITOR_NIVELES_NO_RUNTIME' not in program:
            raise ValueError('Generador no protege las capas de editor. Reinstala parche antes de sincronizar.')
        print('Regenerando mapa Android preservando decoracion y optimizacion...')
        subprocess.run([sys.executable,str(GENERATOR),str(ROOT)],check=True,cwd=ROOT)
    layers=verify_runtime_clean()
    new=text_of(rows)
    DATA.write_text(new,encoding='utf-8')
    print(f'SINCRONIZADO V3: {len(rows)} niveles con desplazamientos RELATIVOS. Runtime sin capas editor ({layers} grupos normales).')
    print(f'Datos ligeros: {DATA.relative_to(ROOT)} ({len(new.encode("utf-8"))} bytes)')
    print('Siguiente: .\\gradlew.bat :android:installDebug --console=plain')


def main():
    p=argparse.ArgumentParser(description=__doc__)
    flags=p.add_mutually_exclusive_group(required=True)
    flags.add_argument('--preparar',action='store_true')
    flags.add_argument('--sincronizar',action='store_true')
    flags.add_argument('--verificar',action='store_true')
    args=p.parse_args()
    if args.preparar:preparar()
    elif args.sincronizar:sincronizar()
    else:
        rows=serialize_changes()
        print('Verificado:',len(rows),'niveles editables, sin cambios de archivos.')
        if DATA.is_file():
            actual=DATA.read_text(encoding='utf-8')
            print('Archivo sincronizado:', actual==text_of(rows))
        if RUNTIME.is_file():print('Capas de editor en APK:', 'NINGUNA' if verify_runtime_clean() else '?')

if __name__=='__main__':
    try:main()
    except (ValueError, OSError, ET.ParseError, subprocess.CalledProcessError) as e:
        sys.exit('ERROR: '+str(e))
