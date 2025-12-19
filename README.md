
# 📌 Reservation System – Test Execution Guide

본 문서는 **회의실 예약 시스템(reservation-system)** 프로젝트의  
**Unit Test / Integration Test 실행 방법** 및 **결과 확인 방법**을 정리한 문서입니다.

⚠️ **본 작업은 테스트 코드 작성 및 실행에 한정되며,  
기존 백엔드 소스코드는 변경하지 않았습니다.**

---

## 1. 프로젝트 개요

### 프로젝트명
- `reservation-system`

### 목적
- 회의실 **예약 취소 / 조회 로직**에 대한 테스트 검증
- **Service 단위(Unit Test)** 및 **통합 테스트(Integration Test)** 수행

### 테스트 범위
- 회의실 예약 취소
    - 대표자 취소
    - 비대표자 취소
    - 예외 케이스
- 회의실 예약 조회
    - 예약 존재
    - 예약 없음
    - 예약 취소 후 조회 반영 여부

### 테스트 환경
- JUnit 5
- Gradle
- H2 In-memory Database
- Spring Boot Test

---

## 2. 프로젝트 실행 명령어 (필수)

프로젝트 **루트 디렉토리**에서 아래 명령어를 실행합니다.

```bash
./gradlew clean test
````

### 명령어 설명

* `clean` : 기존 빌드 결과 삭제
* `test` : 전체 테스트 코드 실행

### 실행 결과

* 모든 테스트 수행
* PASS / FAIL 결과 콘솔 출력
* 테스트 리포트 자동 생성

---

## 3. 테스트 결과 확인 방법 (필수)

### 3.1 Gradle Test Report 확인

테스트 실행 후 아래 경로에 **HTML 테스트 리포트**가 생성됩니다.

```text
reservation-system/build/reports/tests/test/index.html
```

#### 확인 방법 (Windows 기준)

1. 위 경로로 이동
2. `index.html` 파일에서 **오른쪽 마우스 클릭**
3. **“다음에서 열기”**
4. 원하는 브라우저 선택 (Chrome, Edge 등)

#### 리포트에서 확인 가능한 항목

* 전체 테스트 PASS / FAIL 현황
* 테스트 클래스별 실행 결과
* 실패 테스트의 **ErrorCode 및 Stack Trace**

---

### 3.2 테스트 실행 로그 확인

테스트 실행 중 출력된 로그는 **콘솔** 및 **로그 파일**을 통해 확인할 수 있습니다.

#### 테스트 실행 로그

* 각 테스트 케이스별 PASS / FAIL 로그
* 예외 발생 시 ErrorCode 및 원인 확인 가능

#### 문서 증빙용 로그

* `[TC=UT-ROOM-...] [RESULT=PASS/FAIL]` 형식 로그 사용

---

### 3.3 테스트 실행 로그 파일 저장 방법 (선택)

테스트 실행 시 콘솔 로그를 파일로 저장하여
테스트 문서의 **증빙 자료(test-output.txt)**로 활용할 수 있습니다.

#### ① test-output.txt로 로그 저장

```bash
reservation-system\build\test-output.txt
```

.\gradlew clean test 실행 시 자동으로 위 경로에 로그 저장

**활용 예**

* FAIL 테스트의 Stack Trace 첨부
* ErrorCode / 예외 발생 흐름 캡처
* 교수님 제출용 테스트 로그 증빙

---

## 4. 테스트 코드 설명 (요약)

### 4.1 테스트 유형

#### Unit Test (Service 단위)

* `RoomReservationServiceTest`
* 비즈니스 로직 검증
* ErrorCode 및 예외 처리 검증

#### Integration Test

* Controller + Service + JPA 연동 검증
* 실제 DB 흐름(H2) 기반 테스트

---

### 4.2 테스트 특징

* 기존 백엔드 코드 **수정 없음**
* 테스트 코드만 추가하여 검증 수행
* 실패 테스트의 경우:

    * 기능 오류가 아닌
    * **“오류 메시지 미구현(null)” 결함을 발견**하기 위한 테스트
* FAIL 테스트도 **의도된 결과**이며,

    * 요구사항(SRS)의 **“오류 메시지 명확히 전달” 미충족**을 문서화

---

## 5. 백엔드 소스코드 변경사항 (중요)

❗ **백엔드 소스코드 변경사항 없음**

* 기존 Controller / Service / Repository 코드 일절 수정하지 않음
* 테스트 코드만 추가하여 검증 수행
* 발견된 결함은 **테스트 문서 및 결함 로그로만 기록**

---

## 6. 참고 사항

* 본 테스트는 **프론트엔드 미구현 상태**를 전제로 수행됨
* Postman 등 외부 도구 없이 **자동화 테스트만으로 검증**
* 테스트 결과는 **테스트 문서 및 보고서에 그대로 활용 가능**

---

## 7. 문의

테스트 코드 실행 또는 결과 해석 관련 문의는
**테스트 코드 작성자에게 문의 바랍니다.**

