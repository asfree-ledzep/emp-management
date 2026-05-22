-- FAQ 테이블 및 시퀀스 생성
CREATE SEQUENCE FAQ_SEQ START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE FAQ (
  FAQ_ID     NUMBER PRIMARY KEY,
  CATEGORY   VARCHAR2(100),
  QUESTION   VARCHAR2(500)  NOT NULL,
  KEYWORDS   VARCHAR2(500),
  ANSWER     VARCHAR2(4000) NOT NULL,
  CREATED_BY VARCHAR2(100),
  CREATED_AT DATE DEFAULT SYSDATE
);

-- 샘플 데이터
INSERT INTO FAQ (FAQ_ID, CATEGORY, QUESTION, KEYWORDS, ANSWER, CREATED_BY)
VALUES (FAQ_SEQ.NEXTVAL, '연차/휴가', '연차는 몇 일인가요?',
        '연차,휴가,연차일수,휴가일수,연차개수',
        '연간 15일의 유급 연차가 부여됩니다. 입사 1년 미만인 경우 매월 1일씩 발생합니다.', 'admin');

INSERT INTO FAQ (FAQ_ID, CATEGORY, QUESTION, KEYWORDS, ANSWER, CREATED_BY)
VALUES (FAQ_SEQ.NEXTVAL, '급여', '급여일은 언제인가요?',
        '급여일,월급,급여,봉급,지급일,월급날',
        '매월 25일이 급여 지급일입니다. 25일이 주말인 경우 직전 금요일에 지급됩니다.', 'admin');

INSERT INTO FAQ (FAQ_ID, CATEGORY, QUESTION, KEYWORDS, ANSWER, CREATED_BY)
VALUES (FAQ_SEQ.NEXTVAL, '지출', '식비 한도는 얼마인가요?',
        '식비,밥값,식대,식사비,점심,저녁,식사',
        '식비는 1회 최대 10,000원까지 지원됩니다. 영수증 첨부 후 지출 관리 메뉴에서 신청하세요.', 'admin');

INSERT INTO FAQ (FAQ_ID, CATEGORY, QUESTION, KEYWORDS, ANSWER, CREATED_BY)
VALUES (FAQ_SEQ.NEXTVAL, '지출', '출장비 처리는 어떻게 하나요?',
        '출장비,출장,교통비,숙박비,출장경비,출장신청',
        '국내 출장 교통비는 실비, 숙박비는 1박 70,000원 한도로 지원됩니다. 출장 후 5일 이내 지출 관리 시스템에 등록하세요.', 'admin');

INSERT INTO FAQ (FAQ_ID, CATEGORY, QUESTION, KEYWORDS, ANSWER, CREATED_BY)
VALUES (FAQ_SEQ.NEXTVAL, '복리후생', '경조사 지원이 있나요?',
        '경조사,결혼,장례,축의금,조의금,출산',
        '결혼 축의금 100,000원, 부모님 장례 조의금 100,000원, 본인 출산 50,000원을 지원합니다. 해당 사유 발생 시 인사팀에 신청하세요.', 'admin');

INSERT INTO FAQ (FAQ_ID, CATEGORY, QUESTION, KEYWORDS, ANSWER, CREATED_BY)
VALUES (FAQ_SEQ.NEXTVAL, '연차/휴가', '연차 신청은 어떻게 하나요?',
        '연차신청,휴가신청,연차사용,휴가사용,반차',
        '연차는 최소 3일 전에 팀장에게 구두 보고 후 인사팀에 연차 신청서를 제출하세요. 긴급한 경우 당일 신청도 가능합니다.', 'admin');

COMMIT;
