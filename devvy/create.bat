@echo off
REM -----------------------------------------------------------------
REM Devvy Bot 프로젝트의 빈 파일 및 폴더 구조를 생성하는 배치 스크립트
REM -----------------------------------------------------------------

ECHO.
ECHO  Devvy Bot 프로젝트의 폴더 구조와 빈 파일 생성을 시작합니다...
ECHO.

REM --- 프론트엔드 폴더 생성 ---
IF NOT EXIST "src" (
    mkdir "src"
    ECHO  -  "src" 폴더 생성 완료.
)
IF NOT EXIST "src\components" (
    mkdir "src\components"
    ECHO  -  "src\components" 폴더 생성 완료.
)
IF NOT EXIST "src\services" (
    mkdir "src\services"
    ECHO  -  "src\services" 폴더 생성 완료.
)
IF NOT EXIST "src\styles" (
    mkdir "src\styles"
    ECHO  -  "src\styles" 폴더 생성 완료.
)

REM --- 백엔드 폴더 생성 ---
IF NOT EXIST "backend" (
    mkdir "backend"
    ECHO  -  "backend" 폴더 생성 완료.
)
IF NOT EXIST "backend\controller" (
    mkdir "backend\controller"
    ECHO  -  "backend\controller" 폴더 생성 완료.
)
IF NOT EXIST "backend\service" (
    mkdir "backend\service"
    ECHO  -  "backend\service" 폴더 생성 완료.
)
IF NOT EXIST "backend\vo" (
    mkdir "backend\vo"
    ECHO  -  "backend\vo" 폴더 생성 완료.
)
IF NOT EXIST "backend\mapper" (
    mkdir "backend\mapper"
    ECHO  -  "backend\mapper" 폴더 생성 완료.
)

ECHO.
ECHO  빈 파일 생성을 시작합니다...
ECHO.

REM --- 프론트엔드 파일 생성 ---
type nul > "src\main.js"
type nul > "src\App.vue"
type nul > "src\components\DevvyBot.vue"
type nul > "src\components\ChatTab.vue"
type nul > "src\components\HistoryTab.vue"
type nul > "src\components\FeedbackTab.vue"
type nul > "src\services\devvyService.js"
type nul > "src\styles\devvy.scss"
ECHO  -  프론트엔드 파일 8개 생성 완료.

REM --- 백엔드 파일 생성 ---
type nul > "backend\controller\DevvyController.java"
type nul > "backend\service\DevvyService.java"
type nul > "backend\vo\DevvyVo.java"
type nul > "backend\mapper\DevvyMapper.java"
type nul > "backend\mapper\devvyMapper.xml"
ECHO  -  백엔드 파일 5개 생성 완료.

ECHO.
ECHO.
ECHO  =========================================
ECHO  모든 작업이 성공적으로 완료되었습니다.
ECHO  =========================================
ECHO.

REM 작업 완료 후 창이 바로 닫히지 않도록 잠시 대기
pause
