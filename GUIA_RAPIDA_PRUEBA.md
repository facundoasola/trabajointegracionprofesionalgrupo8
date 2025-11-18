# 🚀 Guía Rápida - Compilar y Probar

## ⚡ Compilar el Proyecto

### 1. Abrir en Android Studio
```bash
cd /Users/lucasgima/Downloads/trabajointegracionprofesionalgrupo8-main
# Abrir con Android Studio
```

### 2. Sincronizar Gradle
- Android Studio detectará automáticamente `build.gradle.kts`
- Click en "Sync Now" cuando aparezca el banner
- Esperar a que descargue todas las dependencias

### 3. Compilar
```bash
# Desde terminal (opcional):
./gradlew build

# O usar Android Studio:
# Build > Make Project (Ctrl+F9 / Cmd+F9)
```

## 📱 Probar la App

### Opción 1: Emulador Android
1. Tools > Device Manager
2. Create Virtual Device
3. Seleccionar Pixel 4 (o cualquier dispositivo)
4. API Level 31 o superior
5. Run > Run 'app' (Shift+F10)

### Opción 2: Dispositivo Físico
1. Activar "Opciones de Desarrollador" en tu teléfono
2. Activar "Depuración USB"
3. Conectar por USB
4. Run > Run 'app'

## 🧪 Flujo de Prueba Completo

### 1. Registro
```
Email: test@saferoute.com
Password: 123456
Nombre: Test
Apellido: Usuario
```

### 2. Login
```
Email: test@saferoute.com
Password: 123456
```

### 3. Explorar Mapa
- ✅ Ver crímenes cargados desde backend
- ✅ Ver zonas de peligro coloreadas
- ✅ Hacer click en marcadores para ver detalles

### 4. Reportar un Crimen
1. Click en "📍 Reportar Crimen"
2. Llenar formulario:
   - **Dirección**: "Av. Corrientes 500, Buenos Aires, Argentina"
   - **Descripción**: "Robo de celular"
   - **Cuándo**: "Hace 1 hora"
   - **Categoría**: "Delitos contra las personas"
   - **Subtipo**: "Robo/Arrebato"
3. Click "Enviar Reporte"
4. Verificar que aparece en el mapa

### 5. Abrir Menú (☰)
- Click en botón de menú (esquina superior izquierda)
- Ver información del usuario y puntos actuales

### 6. Ver Reportes Pendientes
1. Desde menú > "📋 Reportes Pendientes"
2. Ver lista de reportes no confirmados
3. Click "✓ Verificar" en algún reporte
4. Click "✓ Confirmar" para confirmarlo oficialmente

### 7. Ver Mis Reportes
1. Desde menú > "🚨 Mis Reportes"
2. Ver tus reportes con estado
3. Verificar que los confirmados muestran "+10 puntos"

### 8. Ver Puntos
1. Desde menú > "🏆 Ver Mis Puntos"
2. Click "🔄 Actualizar Puntos"
3. Verificar que los puntos se actualizan

## 🐛 Solución de Problemas

### Error de Compilación
```bash
# Limpiar y reconstruir:
./gradlew clean build
```

### Error de Permisos de Internet
- Verificar que `AndroidManifest.xml` tiene:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Backend no responde
- Verificar URL en `ApiConfig.java`:
```java
public static final String BASE_URL = "https://tp-sip-be.onrender.com/api/";
```
- Verificar conexión a internet
- La app tiene fallback a datos hardcodeados si falla

### No aparecen crímenes en el mapa
1. Verificar que el backend tiene datos:
   - Abrir: `https://tp-sip-be.onrender.com/api/crimenes`
   - Debe mostrar JSON con lista de crímenes
2. Verificar logs en Logcat (filtrar por "SafeRoute" o "MainActivity")
3. La app mostrará un Toast si hay error de conexión

### Geocodificación falla
- La app usa Nominatim (OpenStreetMap) para geocodificar
- Requiere direcciones completas de Buenos Aires
- Ejemplo válido: "Av. Corrientes 300, Buenos Aires, Argentina"

## 📊 Verificar Integración Backend

### 1. Probar Endpoints Manualmente

#### Login
```bash
curl -X POST https://tp-sip-be.onrender.com/api/login \
  -H "Content-Type: application/json" \
  -d '{"mail":"test@saferoute.com","password":"123456"}'
```

#### Obtener Crímenes
```bash
curl https://tp-sip-be.onrender.com/api/crimenes
```

#### Crear Crimen
```bash
curl -X POST https://tp-sip-be.onrender.com/api/crimen-nuevo \
  -H "Content-Type: application/json" \
  -d '{
    "type":"Robo",
    "description":"Test",
    "address":"Av. Corrientes 300",
    "latitude":-34.6035,
    "longitude":-58.3794,
    "reporter":"test@saferoute.com"
  }'
```

### 2. Verificar en Logcat (Android Studio)
```
Filtrar por: "MainActivity"
Buscar:
- "✅ X incidentes cargados"
- "Reporte enviado"
- "Error de conexión"
```

## 🎯 Casos de Prueba Exitosos

### ✅ Caso 1: Usuario Nuevo
1. Registrarse
2. Login
3. Ver mapa con crímenes
4. Reportar crimen
5. Puntos = 0 (hasta que se confirme)

### ✅ Caso 2: Verificador
1. Login
2. Ir a Reportes Pendientes
3. Verificar 3 reportes diferentes
4. Ver que las verificaciones aumentan

### ✅ Caso 3: Moderador/Admin
1. Login
2. Ir a Reportes Pendientes
3. Confirmar un reporte con 3+ verificaciones
4. El reportante gana 10 puntos automáticamente

### ✅ Caso 4: Ver Mis Puntos
1. Reportar varios crímenes
2. Esperar confirmaciones
3. Ir a "Ver Mis Puntos"
4. Click "Actualizar"
5. Ver puntos ganados

## 📝 Checklist de Prueba

- [ ] App compila sin errores
- [ ] Login funciona
- [ ] Registro funciona
- [ ] Mapa carga crímenes del backend
- [ ] Reportar crimen funciona (aparece en backend)
- [ ] Menú abre correctamente
- [ ] Ver puntos muestra información correcta
- [ ] Reportes pendientes muestra lista
- [ ] Verificar reporte funciona
- [ ] Confirmar reporte funciona
- [ ] Mis reportes muestra lista filtrada
- [ ] Puntos se actualizan tras confirmación
- [ ] Logout funciona

## 🔗 URLs de Referencia

- **Backend**: https://tp-sip-be.onrender.com
- **API Docs**: (Ver `IMPLEMENTACION_COMPLETA.md`)
- **Geocoding API**: Nominatim (OpenStreetMap)

## 💡 Tips

1. **Primera ejecución**: Puede tardar en cargar crímenes (backend en Render puede estar dormido)
2. **Emulador lento**: Usar AVD con API 31 y Hardware Acceleration
3. **Debugging**: Activar logs verbosos en Logcat
4. **Testing real**: Mejor probar en dispositivo físico con GPS real

---

**¡La app está lista para usar!** 🎉

Si encuentras algún error, revisa los logs y verifica la conexión con el backend.

