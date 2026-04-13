@echo off
:: =============================================================================
::  Hello World Mod - Build Script (Windows)
::
::  使い方:
::    build.bat              # すべてのローダーをビルド
::    build.bat neoforge     # NeoForge のみ
::    build.bat fabric       # Fabric のみ
:: =============================================================================

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set OUTPUT_DIR=%SCRIPT_DIR%dist
set ALL_LOADERS=neoforge fabric
set SUCCEEDED=
set FAILED=

:: ---- ターゲット決定 --------------------------------------------------
if "%~1"=="" (
    set TARGETS=%ALL_LOADERS%
) else (
    set TARGETS=
    :parse_args
    if "%~1"=="" goto :done_parse
    set ARG=%~1
    set VALID=false
    for %%L in (%ALL_LOADERS%) do (
        if /i "!ARG!"=="%%L" (
            set TARGETS=!TARGETS! %%L
            set VALID=true
        )
    )
    if "!VALID!"=="false" (
        echo [ERROR] Unknown loader: '%ARG%'  (valid: neoforge, fabric^)
        exit /b 1
    )
    shift
    goto :parse_args
    :done_parse
)

:: ---- dist フォルダ作成 -----------------------------------------------
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

:: ---- ビルド実行 -------------------------------------------------------
for %%L in (%TARGETS%) do (
    echo ----------------------------------------
    echo [INFO]  Building: %%L
    echo ----------------------------------------

    set LOADER_DIR=%SCRIPT_DIR%%%L

    if not exist "!LOADER_DIR!\gradlew.bat" (
        echo [WARN]  gradlew.bat not found in %%L\ -- run 'gradle wrapper' first.
        set FAILED=!FAILED! %%L
        goto :next
    )

    pushd "!LOADER_DIR!"
    call gradlew.bat build --console=plain
    set BUILD_RESULT=!errorlevel!
    popd

    if !BUILD_RESULT! equ 0 (
        :: JAR を dist/ にコピー（sources・dev JAR を除外）
        for %%J in ("!LOADER_DIR!\build\libs\*.jar") do (
            echo %%~nxJ | findstr /i "sources dev" >nul
            if errorlevel 1 (
                copy "%%J" "%OUTPUT_DIR%\" >nul
                echo [INFO]  Output: dist\%%~nxJ
            )
        )
        set SUCCEEDED=!SUCCEEDED! %%L
    ) else (
        echo [ERROR] Build failed: %%L
        set FAILED=!FAILED! %%L
    )
    :next
)

:: ---- サマリー ---------------------------------------------------------
echo ----------------------------------------
echo [INFO]  Build Summary
echo ----------------------------------------
for %%S in (%SUCCEEDED%) do echo   OK  %%S
for %%F in (%FAILED%)    do echo   NG  %%F
echo ----------------------------------------

if not "%FAILED%"=="" exit /b 1
exit /b 0
