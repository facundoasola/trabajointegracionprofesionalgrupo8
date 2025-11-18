#!/bin/zsh

# Script de Compilación Rápida - SafeRoute
# Ejecutar con: zsh compile_now.sh

echo "🚀 Compilando SafeRoute con las correcciones..."
echo "================================================"
echo ""

cd /Users/lucasgima/Downloads/trabajointegracionprofesionalgrupo8-main || exit 1

echo "✅ Dando permisos a gradlew..."
chmod +x gradlew

echo ""
echo "🧹 Limpiando proyecto anterior..."
./gradlew clean

echo ""
echo "🔨 Compilando (esto tarda 1-2 minutos)..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "================================================"
    echo "✅✅✅ COMPILACIÓN EXITOSA ✅✅✅"
    echo "================================================"
    echo ""
    echo "📱 La app está lista. Ahora:"
    echo ""
    echo "1. Abre Android Studio"
    echo "2. Click en Run 'app' (▶️)"
    echo "3. Login"
    echo "4. Intenta REPORTAR UN CRIMEN"
    echo ""
    echo "🎯 Resultado esperado:"
    echo "   ✅ NO más error 'End of input'"
    echo "   ✅ Toast: 'Reporte enviado exitosamente'"
    echo ""
    echo "📋 Luego prueba los botones:"
    echo "   Menú > Reportes Pendientes"
    echo "   Verás: ❌ No sirve  |  ✅ Confirmar"
    echo ""
else
    echo ""
    echo "❌ ERROR EN LA COMPILACIÓN"
    echo ""
    echo "Por favor comparte el error que aparece arriba ⬆️"
    exit 1
fi

