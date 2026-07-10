# 로컬 개발 환경 설정

## 1. PostgreSQL 설치

### Windows
1. [postgresql.org](https://www.postgresql.org/download/windows/) 에서 PostgreSQL 17 Windows x86-64 다운로드
2. 설치 시 설정:
    - Port: 5432 (기본값 유지)
    - 비밀번호 설정 (반드시 기억해둘 것)
    - Stack Builder: 체크 해제해도 됨
3. 설치 후 환경변수 PATH 추가:
    - 시스템 속성 → 환경 변수 → 시스템 변수 Path → 편집 → 새로 만들기
    - 아래 경로 추가 (기본 설치 경로 확인):
```
   C:\Program Files\PostgreSQL\17\bin
```

### macOS
```bash
brew install postgresql@17
brew services start postgresql@17
```

## 2. DB 생성

1. IntelliJ 오른쪽 **Database** 탭 클릭
2. `+` → **PostgreSQL** 선택
3. 아래와 같이 입력:
    - Host: `localhost`
    - Port: `5432`
    - Database: `postgres` (기본값)
    - User: `postgres`
    - Password: 설치 시 설정한 비밀번호
4. **Download missing driver files** 클릭 (최초 1회)
5. **Test Connection** 후 **OK**
6. 연결된 `postgres` 우클릭 → **New** → **Query Console**
7. 아래 SQL 입력 후 실행:
```sql
CREATE DATABASE safefam;
```
8. Database 탭에서 `postgres` 우클릭 → **Properties** → Database를 `safefam`으로 변경 후 **OK**

## 3. 환경변수 설정

IntelliJ 상단 → **Run** → **Edit Configurations** → **Environment variables** 에 추가:
```
DB_PASSWORD=설치시설정한비밀번호
JWT_SECRET=c2FmZWZhbS1zZWNyZXQta2V5LWZvci1qd3QtYXV0aGVudGljYXRpb24=
```
> ⚠️ JWT_SECRET은 개발용 키입니다. 운영 배포 시 반드시 새로운 키로 교체해야 합니다.

## 4. 실행

Spring Boot Run 버튼 클릭하면 Flyway가 자동으로 테이블 생성
