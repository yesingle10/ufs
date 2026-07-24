{{/*
Expand the name of the chart.
*/}}
{{- define "ufs.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this
(by the DNS naming spec).
*/}}
{{- define "ufs.fullname" -}}
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
{{- define "ufs.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels.
*/}}
{{- define "ufs.labels" -}}
helm.sh/chart: {{ include "ufs.chart" . }}
{{ include "ufs.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels.
*/}}
{{- define "ufs.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ufs.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
MySQL fullname.
*/}}
{{- define "mysql.fullname" -}}
{{- printf "%s-mysql" (include "ufs.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Redis fullname.
*/}}
{{- define "redis.fullname" -}}
{{- printf "%s-redis" (include "ufs.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
ConfigMap name.
*/}}
{{- define "ufs.configmapName" -}}
{{- printf "%s-config" (include "ufs.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Secret name.
*/}}
{{- define "ufs.secretName" -}}
{{- printf "%s-secret" (include "ufs.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
PVC name.
*/}}
{{- define "ufs.pvcName" -}}
{{- printf "%s-data" (include "ufs.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
HPA name.
*/}}
{{- define "ufs.hpaName" -}}
{{- printf "%s-hpa" (include "ufs.fullname" .) | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Service account name (optional, uses default if not set).
*/}}
{{- define "ufs.serviceAccountName" -}}
{{- if .Values.serviceAccount }}
{{- if .Values.serviceAccount.create }}
{{- default (include "ufs.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- else }}
{{- "default" }}
{{- end }}
{{- end }}
