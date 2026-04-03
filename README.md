# Nexis DB setup

## NEXIS Monitoring Platform - DB Setup

에이전트로부터 수집한 값과 웹 설정 등의 데이터를 저장할 DB 테이블을 생성해주는 프로그램입니다.

> *반드시 사전에 Database와 User 생성 후 진행하여야 합니다.
<br><br>

---

## 파일 다운로드

| OS | 파일 | 크기 |
|---|---|---|
| Windows | [`NexisDB_Setup_v1.exe`](https://drive.google.com/file/d/1ymEhIBR5PnaNiablz7NuNkmPou0O7Nkw/view?usp=drive_link) | 11.98MB |
| Linux | [`nexis-db-setup-1.0-1.noarch.rpm`](https://drive.google.com/file/d/1m7vPPSII7CQ-GoMT5WW50CQLrxRZWuKM/view?usp=drive_link) | 9.10MB |

<br><br>

---

## 설치 방법 (Windows)

> 자세한 내용은 [티스토리 참조](https://hailey-p.tistory.com/21)

<br><br>

1. `NexisDB_Setup_v1.exe` 실행 *(관리자 권한으로 실행)*

2. 설치 경로 지정

3. 시작 메뉴 폴더 지정

4. 설치 전 선택 항목 확인

5. 설치 진행 화면

6. 설치 완료 화면

7. 프로그램 실행 *(관리자 권한으로 실행)*

8. 정보 입력 후 Test Connection

9. Initialize Database 클릭하여 테이블 생성

10. DB에서 테이블 생성 확인 가능

<br><br>

---

## 설치 방법 (Linux)

1. `nexis-db-setup-1.0-1.noarch.rpm` 파일 확인

2. rpm 설치
```bash
rpm -ivh nexis-db-setup-1.0-1.noarch.rpm
```

3. 설치 완료 후 테이블 생성 확인
