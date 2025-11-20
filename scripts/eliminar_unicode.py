import os
import glob

# Diccionario de reemplazos
replacements = {
    '╔': '+',
    '╗': '+',
    '╚': '+',
    '╝': '+',
    '╠': '+',
    '╣': '+',
    '║': '|',
    '═': '=',
    '★': '*',
    '✓': 'OK',
    '✗': 'X',
    '►': '>',
    '▶': '>',
    '●': '*',
    '■': '#',
    '◆': '*',
    '¿': '',  # Quitar signos de interrogación de apertura
    '¡': ''   # Quitar signos de exclamación de apertura
}

# Encontrar todos los archivos .java
java_files = []
for root, dirs, files in os.walk('src'):
    for file in files:
        if file.endswith('.java'):
            java_files.append(os.path.join(root, file))

print(f"Encontrados {len(java_files)} archivos .java")

modified_count = 0

for filepath in java_files:
    print(f"\nProcesando: {filepath}")
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # Aplicar reemplazos
        for old, new in replacements.items():
            if old in content:
                count = content.count(old)
                print(f"  - Reemplazando '{old}' ({count} veces)")
                content = content.replace(old, new)
        
        # Solo escribir si hubo cambios
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"  -> MODIFICADO")
            modified_count += 1
        else:
            print(f"  -> Sin cambios")
            
    except Exception as e:
        print(f"  -> ERROR: {e}")

print(f"\n{'='*50}")
print(f"Proceso completado!")
print(f"Archivos modificados: {modified_count}/{len(java_files)}")
