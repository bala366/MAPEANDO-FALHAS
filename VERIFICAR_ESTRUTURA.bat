@echo off
setlocal
cd /d "%~dp0"
echo.
echo =============================================
echo VERIFICANDO ESTRUTURA DO PROJETO
ECHO =============================================
set ERRO=0
for %%F in (settings.gradle build.gradle gradle.properties app\build.gradle app\src\main\AndroidManifest.xml app\src\main\java\com\lotofacil\mapafalhas\MainActivity.java .github\workflows\build-apk.yml) do (
  if exist "%%F" (
    echo OK   %%F
  ) else (
    echo ERRO %%F NAO ENCONTRADO
    set ERRO=1
  )
)
if "%ERRO%"=="0" (
  echo.
  echo ESTRUTURA OK PARA SUBIR AO GITHUB.
) else (
  echo.
  echo EXISTEM ARQUIVOS FALTANDO.
)
pause
