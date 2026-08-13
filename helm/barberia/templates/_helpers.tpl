{{/*
Expand the name of the chart.
*/}}
{{- define "barberia.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "barberia.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "barberia.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "barberia.labels" -}}
helm.sh/chart: {{ include "barberia.chart" . }}
{{ include "barberia.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "barberia.selectorLabels" -}}
app.kubernetes.io/name: {{ include "barberia.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "barberia.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "barberia.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Deployment + Service de un componente cualquiera de la barberia.

Los seis componentes tenian su propio fichero con el mismo YAML copiado, y las
variables de entorno se listaban una a una a mano. Eso ya habia costado un fallo:
al anadir una variable a values.yaml no llegaba al pod hasta acordarse de tocar
tambien el template. Aqui el bloque env: se genera recorriendo el mapa, asi que
lo que este en values.yaml llega siempre.

Parametros (dict):
  nombre   - nombre del Deployment y del Service (el DNS que ven los demas pods)
  etiqueta - valor de la etiqueta 'service', usada por el selector
  cfg      - subarbol de .Values.servicios.<x>
  replicas - .Values.replicaCount
  tag      - .Values.imageTag
*/}}
{{- define "barberia.componente" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .nombre }}
  labels:
    app: barberia
    service: {{ .etiqueta }}
spec:
  replicas: {{ .replicas }}
  selector:
    matchLabels:
      app: barberia
      service: {{ .etiqueta }}
  template:
    metadata:
      labels:
        app: barberia
        service: {{ .etiqueta }}
    spec:
      containers:
        - name: {{ .etiqueta }}
          image: "{{ .cfg.image }}:{{ .tag }}"
          # OJO: el tag NO puede ser fijo, y menos 'latest'.
          #
          # El Kubernetes de Docker Desktop no usa el demonio Docker, usa
          # containerd, que tiene su propio almacen de imagenes. Docker Desktop
          # las trasvasa desde el demonio, pero SOLO cuando kubelet hace un pull
          # de verdad. Con un tag fijo y esta politica, kubelet ve que ya tiene
          # 'latest' guardado, no pide nada, y sigue arrancando la imagen vieja
          # por mucho que se reconstruya: los cambios en el JAR nunca llegaban
          # al cluster. Con un tag nuevo en cada build (lo pone desplegar.ps1)
          # el tag no esta en cache, kubelet pide la imagen y recibe la recien
          # construida.
          #
          # IfNotPresent y no Always porque estas imagenes no viven en ningun
          # registro: con Always kubelet intentaria descargarlas de Docker Hub
          # y el pod se quedaria en ErrImagePull.
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: {{ .cfg.containerPort }}
          {{- with .cfg.env }}
          env:
            {{- range $clave, $valor := . }}
            - name: {{ $clave }}
              value: {{ $valor | quote }}
            {{- end }}
          {{- end }}
          # Sonda de TCP y no de HTTP a proposito: los microservicios de negocio
          # no traen actuator, asi que no hay un /health que consultar. Lo que
          # importa aqui es que el Service no reciba endpoints hasta que la JVM
          # tenga el puerto abierto; sin esto, 'kubectl get pods' decia Running
          # y las primeras peticiones del gateway morian en connection refused.
          readinessProbe:
            tcpSocket:
              port: {{ .cfg.containerPort }}
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30
---
apiVersion: v1
kind: Service
metadata:
  name: {{ .nombre }}
  labels:
    app: barberia
    service: {{ .etiqueta }}
spec:
  selector:
    app: barberia
    service: {{ .etiqueta }}
  ports:
    - port: {{ .cfg.servicePort }}
      targetPort: {{ .cfg.containerPort }}
      {{- if and (eq (.cfg.serviceType | default "ClusterIP") "NodePort") .cfg.nodePort }}
      nodePort: {{ .cfg.nodePort }}
      {{- end }}
  # ClusterIP por defecto: los microservicios solo hablan entre ellos y no tienen
  # por qué asomarse fuera del clúster. El gateway es la excepción y lo declara
  # en values.yaml.
  type: {{ .cfg.serviceType | default "ClusterIP" }}
{{- end }}
