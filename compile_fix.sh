#!/bin/zsh

# Script para dar permisos y compilar SafeRoute App
# Ejecutar con: zsh compile_fix.sh

echo "🔧 Arreglando permisos y compilando SafeRoute App"
echo "=================================================="
echo ""

# Ir al directorio del proyecto
cd /Users/lucasgima/Downloads/trabajointegracionprofesionalgrupo8-main || exit 1

echo "✅ Directorio: $(pwd)"
echo ""

echo "📝 Paso 1: Dando permisos de ejecución a gradlew..."
chmod +x gradlew
echo "✅ Permisos otorgados"
echo ""

echo "🧹 Paso 2: Limpiando proyecto..."
./gradlew clean
echo "✅ Proyecto limpiado"
echo ""

echo "🔨 Paso 3: Compilando proyecto (esto puede tardar 2-3 minutos)..."
./gradlew build -x test
BUILD_STATUS=$?

echo ""
echo "=================================================="

if [ $BUILD_STATUS -eq 0 ]; then
    echo "✅✅✅ COMPILACIÓN EXITOSA ✅✅✅"
    echo ""
    echo "🎉 El proyecto está listo!"
    echo ""
    echo "Próximos pasos:"
    echo "1. Abre Android Studio"
    echo "2. Ejecuta la app (▶️ Run 'app')"
    echo "3. Abre Logcat (Cmd+6)"
    echo "4. Haz login y observa los logs"
    echo ""
    echo "Si crashea, copia TODOS los logs que veas de:"
    echo "  - D/MainActivity:"
    echo "  - E/MainActivity:"
    echo "  - E/AndroidRuntime:"
    echo ""
else
    echo "❌ ERROR EN COMPILACIÓN"
    echo ""
    echo "Verifica los errores arriba ⬆️"
    echo ""
    exit 1
fi

