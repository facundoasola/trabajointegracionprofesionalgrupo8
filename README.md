# SafeRouteApp - Aplicación de Rutas Seguras

## Descripción
SafeRouteApp es una aplicación Android que ayuda a los usuarios a planificar rutas seguras en Buenos Aires, Argentina, evitando zonas de alta criminalidad. La aplicación utiliza mapas interactivos con alertas de crimen en tiempo real y calcula rutas optimizadas tanto para peatones como para vehículos.

---

## 🚀 Características Principales

### 1. **Sistema de Mapas Interactivo**
- Integración con **OSMDroid** y tiles de **Mapbox**
- Visualización de mapas de Buenos Aires con controles de zoom
- Marcador de ubicación actual del usuario (Av. Santa Fe 995)
- Marcador animado con efecto de pulso para la ubicación actual

### 2. **Alertas de Crimen Categorizadas**
- **11 alertas de crimen** distribuidas en Capital Federal
- **Dos categorías principales:**
  - Delitos contra las personas (crímenes en vía pública)
  - Delitos contra la propiedad (robos de vehículos)

#### Sistema de Gravedad (4 niveles)
- **Nivel 1 (Leve)**: Hurto - Color amarillo dorado
- **Nivel 2 (Moderado)**: Robo/Arrebato - Color naranja
- **Nivel 3 (Grave)**: Agresión grave, Robo vehículo estacionado - Color rojo
- **Nivel 4 (Muy Grave)**: Homicidio, Robo con arma - Color rojo oscuro

### 3. **Zonas de Calor Dinámicas**
Las zonas de calor varían según la gravedad del crimen:
- **Nivel 1**: Radio de 40m, muy transparente
- **Nivel 2**: Radio de 80m, moderadamente visible
- **Nivel 3**: Radio de 180m, visible
- **Nivel 4**: Radio de 250m, más opaco

Los colores se han optimizado para no ocultar el mapa base.

### 4. **Cálculo de Rutas Inteligentes**
- **Dos modos de transporte:**
  - 🚶 Modo peatón
  - 🚗 Modo vehículo
  
- **Dos tipos de ruta:**
  - 🛡️ **Ruta Segura**: Evita zonas de alto riesgo mediante waypoints
  - ⚡ **Ruta Rápida**: Ruta directa más corta

#### Algoritmo de Seguridad
- Radio de influencia ajustado según gravedad del crimen
- Multiplicador por gravedad: nivel 4 tiene 3x el radio de nivel 1
- Cálculo de riesgo con decaimiento exponencial según distancia
- Optimización específica por modo de transporte

### 5. **Filtros de Visualización**
- **Botón rojo** 🚶: Filtrar crímenes contra peatones/transeúntes
- **Botón violeta** 🚗: Filtrar robos de vehículos
- Las zonas de calor se mantienen sincronizadas con los filtros

### 6. **Animaciones**
- Marcadores de alertas con animación de transparencia fluida (100% a 50%)
- Marcador de ubicación actual con pulso de luz
- Transiciones suaves en la interfaz

### 7. **Exportación a Apps de Transporte**
- **Modo peatón**: Botón "Exportar a Pedidos Ya"
- **Modo vehículo**: Botones "Exportar a Uber" y "Exportar a Pedidos Ya"
- Integración con deep links de las aplicaciones

### 8. **Reportar Crimen**
- Formulario completo con:
  - Selección de categoría (desplegable)
  - Selección de subtipo (dinámico según categoría)
  - Asignación automática de gravedad
  - Campo de descripción opcional
  - Dirección del incidente
  - Hora del incidente
  - Opción de adjuntar imagen (simulado)

### 9. **Puntos de Seguridad**
- **11 hospitales públicos** de CABA
- **15 comisarías vecinales** distribuidas por comunas
- Iconos diferenciados para cada tipo

---

## 🛠️ Tecnologías Utilizadas

### Mapas y Navegación
- **OSMDroid 6.1.14**: Biblioteca de mapas Open Street Map
- **Mapbox**: Tiles de mapa de alta calidad
- **GraphHopper API**: Cálculo de rutas con alternativas

### Android
- **Target SDK**: 36 (Android 16+)
- **Min SDK**: 24
- **Lenguaje**: Java
- **Build System**: Gradle 9.0-milestone-1
- **AGP**: 8.13.1

### Componentes UI
- Material Design Components
- AndroidX Libraries
- Alertas personalizadas (AlertDialog)

---

## 📊 Datos de Crimen

### Delitos contra las Personas (7 alertas)
1. **Av. Corrientes 300** - Robo a mano armada (Nivel 2)
2. **Av. Corrientes 400** - Homicidio (Nivel 4)
3. **Florida 350** - Robo con intimidación (Nivel 2)
4. **Florida 450** - Agresión grave (Nivel 3)
5. **Sarmiento 400** - Robo en taxi (Nivel 2)
6. **Lavalle 600** - Hurto por distracción (Nivel 1)
7. **Av. Santa Fe 800** - Robo nocturno (Nivel 2)

### Delitos contra la Propiedad (4 alertas)
1. **Av. Corrientes 450** - Robo de automóvil (Nivel 3)
2. **Lavalle 500** - Robo de bicicleta (Nivel 2)
3. **Av. Santa Fe 750** - Robo de motocicleta (Nivel 3)
4. **Florida 550** - Robo con arma a conductor (Nivel 4)

---

## 🎨 Paleta de Colores

### Alertas y Filtros
- **Rojo (#F44336)**: Crímenes contra peatones
- **Violeta (#9C27B0)**: Robos de vehículos
- **Azul (#4285F4)**: Ubicación actual del usuario

### Zonas de Calor
- **Amarillo dorado (#FFD700)**: Nivel 1
- **Naranja (#FFA500)**: Nivel 2
- **Rojo claro (#FF4444)**: Nivel 3
- **Rojo intenso (#CC0000)**: Nivel 4

### Rutas
- **Azul/Verde**: Ruta segura (según modo)
- **Rojo/Naranja**: Ruta rápida (según modo)

---

## 🔧 Configuración del Proyecto

### Requisitos
- Android Studio Arctic Fox o superior
- JDK 11 o superior
- Gradle 9.0+
- Dispositivo Android con API 24+ o emulador

### Claves API
- **Mapbox Access Token**: Configurado en `MainActivity.java`
- **GraphHopper API Key**: Configurado para cálculo de rutas

### Compilación
```bash
# Compilar versión debug
.\gradlew.bat assembleDebug

# Instalar en dispositivo
.\gradlew.bat installDebug
```

---

## 📱 Funcionalidades de UI

### Panel de Búsqueda
- Campo de origen (auto-completa con ubicación actual)
- Campo de destino
- Botón "Trazar Rutas" (peatón)
- Botón "Ruta en Vehículo"

### Panel de Información de Ruta
- Selector de ruta segura vs. rápida
- Información de tiempo y distancia
- Botones de exportación a apps de transporte
- Botón de retroceso para limpiar ruta

### Controles de Mapa
- Zoom in/out
- Filtros de crimen (2 botones)
- Botón de menú (superior izquierda)
- Botón "Reportar Crimen" (superior derecha)

---

## 🔐 Sistema de Autenticación

Implementado con credenciales hardcodeadas para demostración:
- `LoginActivity.java`
- `RegisterActivity.java`

---

## 📋 Arquitectura del Código

### Clases Principales

#### `MainActivity.java` (2,391 líneas)
- Gestión del mapa y overlays
- Cálculo de rutas seguras
- Sistema de alertas de crimen
- Filtros y visualizaciones

#### Clases Internas
- `CrimeAlert`: Modelo de datos para alertas
- `SafePoint`: Puntos de seguridad (hospitales, comisarías)
- `RouteInfo`: Información de ruta calculada
- `RouteWithSafety`: Ruta con puntuación de seguridad

### Métodos Clave
- `calculateBothRoutes()`: Calcula rutas segura y rápida
- `getSafeRoute()`: Genera ruta con waypoints seguros
- `createDangerZones()`: Crea zonas de calor por gravedad
- `calculatePointRisk()`: Evalúa riesgo de un punto
- `isPointNearDanger()`: Detecta proximidad a peligros

---

## 🎯 Algoritmo de Ruta Segura

### Generación de Waypoints
1. Divide la ruta en segmentos
2. Verifica proximidad a zonas peligrosas
3. Si detecta peligro, busca punto seguro alternativo
4. Ajusta radio de búsqueda según gravedad del crimen

### Cálculo de Riesgo
```
Radio de influencia = 40m + (gravedad × 53.33m)
- Nivel 1: ~93m
- Nivel 2: ~147m
- Nivel 3: ~200m
- Nivel 4: ~200m (máximo)
```

### Peso de Riesgo
```
Peso base = gravedad × 12.5
- Nivel 1: 12.5
- Nivel 4: 50.0 (4x más riesgo)
```

---

## 🐛 Correcciones Implementadas

### Gradle y Build
- ✅ Configurado lint para permitir warnings
- ✅ Eliminado método `onBackPressed()` deprecado
- ✅ Implementado `OnBackPressedCallback` moderno

### Geocodificación
- ✅ Eliminadas alertas fuera de Capital Federal
- ✅ API Nominatim para geocodificación precisa

### Ubicación del Usuario
- ✅ Coordenadas corregidas: (-34.595183687496146, -58.3811805650211)
- ✅ Marcador visible con tamaño aumentado (48dp)

### Zonas de Calor
- ✅ Creación después de geocodificación
- ✅ Sincronización con filtros
- ✅ Colores optimizados para visibilidad

---

## 📝 Notas de Desarrollo

### Limitaciones Conocidas
- Ubicación actual hardcodeada (no usa GPS real)
- Alertas de crimen simuladas (no hay backend)
- Sistema de autenticación básico
- Reportes de crimen no persistentes

### Próximas Mejoras Potenciales
- Integración con API real de crimen
- Backend para persistencia de datos
- Autenticación con Firebase
- Ubicación GPS real
- Notificaciones push para nuevas alertas
- Historial de rutas
- Compartir rutas

---

## 📄 Licencia
Proyecto educativo - Universidad

## 👥 Autores
Grupo Final - Desarrollo de Aplicaciones Móviles

---

## 📞 Contacto
Para consultas sobre el proyecto, contactar al equipo de desarrollo.

---

**Última actualización**: 17 de Noviembre de 2025
