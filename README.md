# demo-backend — Spring Boot en ECS Fargate

Backend REST mínimo pensado para desplegarse en ECS Fargate detrás de un
Application Load Balancer, y ser consumido por un frontend Angular/Vue
servido desde CloudFront + S3.

## Endpoints

| Método | Ruta                  | Descripción                              |
|--------|-----------------------|-------------------------------------------|
| GET    | /api/v1/greeting?name=X | Devuelve un saludo en JSON. Útil para probar conectividad y CORS desde el front. |
| GET    | /api/v1/items          | Lista de items de ejemplo.                |
| GET    | /api/v1/items/{id}     | Item por id.                              |
| POST   | /api/v1/items          | Crea un item nuevo.                       |
| GET    | /actuator/health       | Health check genérico.                    |
| GET    | /actuator/health/readiness | Health check de "listo para tráfico". Es el que debe usar el ALB y el healthCheck del task definition. |

## Ejecutar en local

```bash
mvn spring-boot:run
# o, si prefieres probar el jar empaquetado:
mvn clean package
java -jar target/demo-backend-1.0.0.jar
```

Por defecto acepta peticiones CORS desde `http://localhost:4200` (Angular)
y `http://localhost:5173` (Vite/Vue). Desde tu app frontend, simplemente:

```javascript
// Ejemplo de llamada desde Angular (HttpClient) o fetch nativo
fetch('http://localhost:8080/api/v1/items')
  .then(res => res.json())
  .then(items => console.log(items));
```

## Construir y publicar la imagen en ECR

```bash
# 1. Crear el repositorio en ECR (una sola vez)
aws ecr create-repository --repository-name demo-backend --region <REGION>

# 2. Autenticar Docker contra ECR
aws ecr get-login-password --region <REGION> \
  | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com

# 3. Construir la imagen (multi-stage: no necesitas tener Maven instalado en local)
docker build -t demo-backend .

# 4. Etiquetar y subir
docker tag demo-backend:latest <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/demo-backend:latest
docker push <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/demo-backend:latest
```

Este es exactamente el paso que automatizarías en el job "Build de
artefactos" del pipeline de GitHub Actions que vimos antes, sustituyendo
los pasos manuales de `aws ecr` por la action `aws-actions/amazon-ecr-login`.

## Registrar el task definition y desplegar

```bash
# Sustituye antes los placeholders <ACCOUNT_ID>, <REGION> en ecs-task-definition.json
aws ecs register-task-definition --cli-input-json file://ecs-task-definition.json

# Si el servicio ya existe (creado previamente vía Terraform/CDK), solo
# necesitas forzar que tome la nueva revisión del task definition:
aws ecs update-service \
  --cluster demo-cluster \
  --service demo-backend-service \
  --task-definition demo-backend \
  --force-new-deployment
```

En un pipeline real, este `register-task-definition` + `update-service`
es justo lo que ejecuta la action `amazon-ecs-deploy-task-definition` de
GitHub Actions, o lo que hace `aws_ecs_service` en Terraform cuando
cambias el tag de la imagen en el `task_definition`.

## Cosas a revisar antes de pasar esto a producción

- **Target group del ALB**: configúralo con health check path
  `/actuator/health/readiness`, no `/actuator/health` a secas — así el ALB
  solo manda tráfico a tareas que de verdad están listas (por ejemplo, tras
  un arranque en frío de la conexión a Aurora).
- **Autoscaling del servicio ECS**: define una política de Target Tracking
  sobre `ECSServiceAverageCPUUtilization` (objetivo típico 60-70%), no dejes
  el `desiredCount` fijo en producción.
- **Conexión a Aurora**: cuando añadas Spring Data JPA, configura el
  datasource con `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y
  `SPRING_DATASOURCE_PASSWORD` como variables de entorno/secrets (ya
  preparado en `ecs-task-definition.json`), y usa un pool de conexiones
  (HikariCP, incluido por defecto) con `maximum-pool-size` bajo si usas
  Aurora Serverless v2, para no agotar las ACUs con conexiones ociosas.
- **mvn dependency:go-offline** en el Dockerfile hace que la capa de
  dependencias se cachee; si añades una dependencia nueva al pom.xml, el
  build tardará un poco más esa vez, pero los siguientes builds volverán
  a ser rápidos.
