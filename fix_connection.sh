#!/bin/zsh

# Script de Solución de Timeouts - SafeRoute App
# Ejecutar con: zsh fix_connection.sh

echo "🔧 SOLUCIÓN DE TIMEOUTS - SafeRoute App"
echo "=========================================="
echo ""

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[1;34m'
NC='\033[0m' # No Color

PROJECT_DIR="/Users/lucasgima/Downloads/trabajointegracionprofesionalgrupo8-main"
BACKEND_URL="https://tp-sip-be.onrender.com"

echo "📁 Directorio del proyecto: $PROJECT_DIR"
echo "🌐 Backend URL: $BACKEND_URL"
echo ""
echo -e "${YELLOW}⚠️  NOTA: Este es un servidor GRATUITO que se duerme.${NC}"
echo -e "${YELLOW}   La primera request puede tardar hasta 90 segundos.${NC}"
echo ""

cd "$PROJECT_DIR" || exit 1

echo -e "${BLUE}1️⃣  Verificando si el backend está dormido...${NC}"
echo ""

# Probar conexión rápida al backend
echo "🔍 Probando conexión rápida (10 segundos)..."
if timeout 10s curl -s --connect-timeout 10 "$BACKEND_URL" > /dev/null; then
    echo -e "${GREEN}✅ Backend está DESPIERTO y responde rápidamente${NC}"
    BACKEND_STATUS="awake"
else
    echo -e "${YELLOW}😴 Backend está DORMIDO o muy lento${NC}"
    echo -e "${YELLOW}💡 Esto es normal en servidores gratuitos${NC}"
    BACKEND_STATUS="sleeping"
fi

echo ""
echo -e "${BLUE}2️⃣  Intentando despertar el backend...${NC}"
echo ""

if [ "$BACKEND_STATUS" = "sleeping" ]; then
    echo "🌅 Enviando request para despertar el servidor..."
    echo "⏳ Esto puede tardar hasta 90 segundos..."

    HTTP_CODE=$(timeout 90s curl -s -o /dev/null -w "%{http_code}" --connect-timeout 60 "$BACKEND_URL/api/crimenes")

    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "404" ]; then
        echo -e "${GREEN}✅ ¡Backend despierto! Código: $HTTP_CODE${NC}"
        echo -e "${GREEN}🎉 Ahora las requests serán rápidas${NC}"
    else
        echo -e "${RED}❌ Backend no respondió en 90 segundos${NC}"
        echo -e "${YELLOW}💡 Intenta ejecutar la app de todas formas${NC}"
    fi
else
    echo -e "${GREEN}✅ Backend ya estaba despierto, no hay que esperar${NC}"
fi

echo ""
echo -e "${BLUE}3️⃣  Compilando con mejoras de timeout...${NC}"
echo ""

# Dar permisos
chmod +x gradlew

# Limpiar y compilar
echo "🧹 Limpiando proyecto..."
./gradlew clean > /tmp/gradle_clean.log 2>&1

echo "🔨 Compilando con mejoras de timeout..."
./gradlew assembleDebug > /tmp/gradle_build.log 2>&1

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅✅✅ COMPILACIÓN EXITOSA ✅✅✅${NC}"
    echo ""
    echo -e "${GREEN}🎉 MEJORAS DE TIMEOUT IMPLEMENTADAS:${NC}"
    echo "   ✅ Timeout aumentado a 60-90 segundos"
    echo "   ✅ Mensajes informativos sobre servidores gratuitos"
    echo "   ✅ Helper para despertar backend automáticamente"
    echo "   ✅ Manejo inteligente de SocketTimeoutException"
    echo "   ✅ Logs detallados para debugging"
    echo "   ✅ UX mejorada para timeouts largos"
    echo ""
    echo -e "${BLUE}📱 CÓMO USAR LA APP:${NC}"
    echo ""
    echo -e "${YELLOW}🕐 PRIMERA VEZ DEL DÍA:${NC}"
    echo "   1. Abre la app"
    echo "   2. Intenta login/registro"
    echo "   3. ESPERA hasta 90 segundos si aparece el spinner"
    echo "   4. Una vez que funcione, todo será rápido"
    echo ""
    echo -e "${YELLOW}🔄 SI APARECE TIMEOUT:${NC}"
    echo "   1. NO cerrar la app"
    echo "   2. Esperar 1-2 minutos"
    echo "   3. Intentar de nuevo"
    echo "   4. La segunda vez debería ser rápida"
    echo ""
    echo -e "${BLUE}📋 EN LOGCAT VERÁS:${NC}"
    echo "   📤 'Enviando request de registro'"
    echo "   ⏳ 'Los servidores gratuitos pueden tardar...'"
    echo "   📥 'Respuesta recibida. Código: 200' (éxito)"
    echo "   ❌ 'SocketTimeoutException: timeout' (normal la primera vez)"
    echo ""
else
    echo -e "${RED}❌ ERROR EN LA COMPILACIÓN${NC}"
    echo ""
    echo "Ver errores en:"
    echo "   /tmp/gradle_build.log"
    echo ""
    tail -20 /tmp/gradle_build.log
fi

echo ""
echo "=========================================="
echo -e "${BLUE}🎯 RESUMEN DE TIMEOUT MANAGEMENT${NC}"
echo "=========================================="
echo ""
echo -e "${GREEN}✅ Si el backend estaba despierto:${NC}"
echo "   → La app funcionará normalmente"
echo ""
echo -e "${YELLOW}⏳ Si el backend estaba dormido:${NC}"
echo "   → Primera request: 30-90 segundos"
echo "   → Requests siguientes: rápidas"
echo ""
echo -e "${RED}❌ Si sigue sin funcionar:${NC}"
echo "   → Backend puede estar realmente caído"
echo "   → Intenta en 15-30 minutos"
echo "   → O verifica en navegador: $BACKEND_URL"
echo ""
echo -e "${BLUE}📚 Documentación completa en:${NC}"
echo "   TIMEOUT_SOLUCIONADO.md"
