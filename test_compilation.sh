#!/bin/bash

# Script de Testing Automático para SafeRoute App
# Este script verifica que el proyecto compile correctamente

echo "=========================================="
echo "🧪 TESTING AUTOMÁTICO - SafeRoute App"
echo "=========================================="
echo ""

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Directorio del proyecto
PROJECT_DIR="/Users/lucasgima/Downloads/trabajointegracionprofesionalgrupo8-main"

echo "📁 Directorio del proyecto: $PROJECT_DIR"
echo ""

# Verificar que el directorio existe
if [ ! -d "$PROJECT_DIR" ]; then
    echo -e "${RED}❌ ERROR: Directorio del proyecto no encontrado${NC}"
    exit 1
fi

cd "$PROJECT_DIR" || exit 1

echo "1️⃣  Verificando archivos críticos..."
echo ""

# Verificar archivos clave
files_to_check=(
    "app/src/main/java/com/example/saferouteapp/MainActivity.java"
    "app/src/main/java/com/example/saferouteapp/LoginActivity.java"
    "app/src/main/java/com/example/saferouteapp/ApiService.java"
    "app/src/main/res/layout/activity_main.xml"
    "app/src/main/AndroidManifest.xml"
)

all_files_exist=true
for file in "${files_to_check[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✅${NC} $file"
    else
        echo -e "${RED}❌${NC} $file - NO ENCONTRADO"
        all_files_exist=false
    fi
done

echo ""

if [ "$all_files_exist" = false ]; then
    echo -e "${RED}❌ ERROR: Faltan archivos críticos${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Todos los archivos críticos existen${NC}"
echo ""

echo "2️⃣  Verificando permisos de Gradle..."
echo ""

# Dar permisos de ejecución a gradlew
if [ -f "gradlew" ]; then
    chmod +x gradlew
    echo -e "${GREEN}✅${NC} Permisos de gradlew configurados"
else
    echo -e "${RED}❌${NC} gradlew no encontrado"
    exit 1
fi

echo ""

echo "3️⃣  Limpiando proyecto..."
echo ""

# Limpiar proyecto
./gradlew clean > /tmp/gradle_clean.log 2>&1

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Proyecto limpiado exitosamente${NC}"
else
    echo -e "${RED}❌ Error al limpiar proyecto${NC}"
    echo "Ver log en: /tmp/gradle_clean.log"
    tail -20 /tmp/gradle_clean.log
    exit 1
fi

echo ""

echo "4️⃣  Compilando proyecto (esto puede tardar 2-3 minutos)..."
echo ""

# Compilar proyecto
./gradlew build -x test > /tmp/gradle_build.log 2>&1 &
BUILD_PID=$!

# Mostrar progreso
while kill -0 $BUILD_PID 2>/dev/null; do
    echo -n "."
    sleep 2
done

wait $BUILD_PID
BUILD_EXIT_CODE=$?

echo ""
echo ""

if [ $BUILD_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅✅✅ COMPILACIÓN EXITOSA ✅✅✅${NC}"
    echo ""
    echo "El proyecto compila correctamente."
    echo ""
    echo "📱 Próximo paso:"
    echo "   1. Abre Android Studio"
    echo "   2. Ejecuta la app (Run > Run 'app')"
    echo "   3. Haz login"
    echo ""
    echo "Si crashea, comparte los logs de Logcat que digan:"
    echo "   D/MainActivity: ..."
    echo "   E/MainActivity: ..."
    echo ""
else
    echo -e "${RED}❌ ERROR DE COMPILACIÓN${NC}"
    echo ""
    echo "Ver errores completos en: /tmp/gradle_build.log"
    echo ""
    echo "Últimas 30 líneas del error:"
    echo "----------------------------------------"
    tail -30 /tmp/gradle_build.log
    echo "----------------------------------------"
    echo ""
    exit 1
fi

echo "=========================================="
echo "🎉 TESTING COMPLETADO"
echo "=========================================="

