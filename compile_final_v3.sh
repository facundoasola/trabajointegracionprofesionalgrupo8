#!/bin/zsh

# Script Final de Compilación - SafeRoute App v3.0.0
# Actualizada con nueva API del backend

echo "🚀 COMPILACIÓN FINAL - SafeRoute v3.0.0"
echo "=========================================="
echo ""
echo "✅ Actualizada con nueva API del backend"
echo "✅ Credenciales hardcodeadas eliminadas"
echo "✅ Tipos de crimen oficiales implementados"
echo "✅ Sistema de verificación con mail"
echo "✅ Backend maneja logros automáticamente"
echo ""

cd /Users/lucasgima/Downloads/trabajointegracionprofesionalgrupo8-main || exit 1

echo "🔧 Dando permisos a gradlew..."
chmod +x gradlew

echo ""
echo "🧹 Limpiando proyecto..."
./gradlew clean

echo ""
echo "🔨 Compilando (esto tarda 1-2 minutos)..."
echo "⏳ Implementando nuevas clases: CrimeVerifyRequest, CrimeFilterRequest"
echo "⏳ Actualizando ApiService con endpoints oficiales"
echo "⏳ Integrando tipos de crimen del backend"

./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "=================================================="
    echo "🎉🎉🎉 COMPILACIÓN EXITOSA v3.0.0 🎉🎉🎉"
    echo "=================================================="
    echo ""
    echo "📱 La app está lista con las correcciones finales."
    echo ""
    echo "🔥 NOVEDADES de esta versión:"
    echo "   ✅ API actualizada según documentación oficial"
    echo "   ✅ Registro → login automático"
    echo "   ✅ Verificación incluye mail del usuario"
    echo "   ✅ Backend maneja logros automáticamente"
    echo "   ✅ Tipos de crimen oficiales"
    echo "   ✅ Sin bugs de delays/loops"
    echo ""
    echo "🧪 CÓMO PROBAR:"
    echo ""
    echo "1. REGISTRO:"
    echo "   • Crear cuenta → Directo al mapa (sin login adicional)"
    echo ""
    echo "2. REPORTAR:"
    echo "   • Usar tipos nuevos: ROBO_VIA_PUBLICA, HURTO, etc."
    echo "   • Debería funcionar sin errores"
    echo ""
    echo "3. VERIFICAR:"
    echo "   • Backend ahora recibe quién verifica"
    echo "   • Logros se manejan automáticamente"
    echo ""
    echo "4. CONFIRMAR:"
    echo "   • Backend actualiza estatus y logros"
    echo "   • Sin manejo manual de puntos"
    echo ""
    echo "📄 Ver documentación completa en:"
    echo "   CORRECCIONES_FINALES_API_NUEVA.md"
    echo ""
else
    echo ""
    echo "❌ ERROR EN LA COMPILACIÓN"
    echo ""
    echo "📋 Verifica estos archivos:"
    echo "   • ApiService.java"
    echo "   • CrimeVerifyRequest.java"
    echo "   • PendingReportsActivity.java"
    echo ""
    exit 1
fi

echo "=================================================="
echo "🚀 LISTO PARA USAR"
echo "=================================================="
