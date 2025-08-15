-- 실시간 주식 시세 스트리밍 시스템 DB 스키마

-- 종목 정보 테이블
CREATE TABLE IF NOT EXISTS stocks (
    stock_code VARCHAR(10) PRIMARY KEY COMMENT '종목코드',
    stock_name VARCHAR(100) NOT NULL COMMENT '종목명',
    market_type VARCHAR(10) NOT NULL COMMENT '시장구분 (KOSPI, KOSDAQ)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_market_type (market_type),
    INDEX idx_stock_name (stock_name)
) COMMENT='종목 기본 정보';

-- 실시간 시세 데이터 테이블
CREATE TABLE IF NOT EXISTS quote_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(10) NOT NULL COMMENT '종목코드',
    price DECIMAL(15,2) NOT NULL COMMENT '현재가',
    volume BIGINT NOT NULL COMMENT '거래량',
    change_amount DECIMAL(15,2) COMMENT '전일 대비 변동금액',
    change_rate DECIMAL(8,4) COMMENT '전일 대비 변동률(%)',
    high_price DECIMAL(15,2) COMMENT '고가',
    low_price DECIMAL(15,2) COMMENT '저가',
    open_price DECIMAL(15,2) COMMENT '시가',
    trade_time TIMESTAMP NOT NULL COMMENT '체결시간',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (stock_code) REFERENCES stocks(stock_code),
    INDEX idx_stock_code_time (stock_code, trade_time),
    INDEX idx_trade_time (trade_time),
    INDEX idx_created_at (created_at)
) COMMENT='실시간 시세 데이터';

-- 실시간 호가 마스터 테이블 (하나의 시점에 대한 전체 호가 정보를 대표)
CREATE TABLE IF NOT EXISTS orderbooks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(10) NOT NULL COMMENT '종목코드',
    quote_time TIMESTAMP(6) NOT NULL COMMENT '호가 수신 시간 (마이크로초까지)',
    sequence_number BIGINT COMMENT '데이터 수신 순서 (중복/순서 검증용)',
    total_bid_volume BIGINT COMMENT '총 매수호가 잔량',
    total_ask_volume BIGINT COMMENT '총 매도호가 잔량',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (stock_code) REFERENCES stocks(stock_code),
    INDEX idx_stock_code_time (stock_code, quote_time),
    INDEX idx_sequence (sequence_number),
    INDEX idx_created_at (created_at)
) COMMENT='실시간 호가 정보 마스터';

-- 실시간 호가 레벨 상세 테이블 (10단계 호가 정보를 각각의 행으로 저장)
CREATE TABLE IF NOT EXISTS orderbook_levels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    orderbook_id BIGINT NOT NULL COMMENT '호가 마스터 ID',
    order_type VARCHAR(4) NOT NULL COMMENT '호가 타입 (BID, ASK)',
    price_level TINYINT NOT NULL COMMENT '호가 순서 (1~10)',
    price DECIMAL(15,2) NOT NULL COMMENT '호가',
    volume BIGINT NOT NULL COMMENT '호가 잔량',

    FOREIGN KEY (orderbook_id) REFERENCES orderbooks(id) ON DELETE CASCADE,
    INDEX idx_orderbook_id (orderbook_id),
    INDEX idx_orderbook_type_level (orderbook_id, order_type, price_level)
) COMMENT='실시간 호가 상세 레벨';

-- 사용자 알림 조건 테이블
CREATE TABLE IF NOT EXISTS notification_conditions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL COMMENT '사용자ID',
    stock_code VARCHAR(10) NOT NULL COMMENT '종목코드',
    condition_type VARCHAR(20) NOT NULL COMMENT '조건타입 (PRICE_ABOVE, PRICE_BELOW, VOLUME_ABOVE)',
    target_value DECIMAL(15,2) NOT NULL COMMENT '목표값',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성화 여부',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (stock_code) REFERENCES stocks(stock_code),
    INDEX idx_user_id (user_id),
    INDEX idx_stock_code_active (stock_code, is_active),
    INDEX idx_condition_type (condition_type)
) COMMENT='사용자 알림 조건';

-- 기본 종목 데이터 삽입
INSERT IGNORE INTO stocks (stock_code, stock_name, market_type) VALUES
('005930', '삼성전자', 'KOSPI'),
('000660', 'SK하이닉스', 'KOSPI'),
('035420', 'NAVER', 'KOSPI'),
('005380', '현대차', 'KOSPI'),
('035720', '카카오', 'KOSPI'),
('207940', '삼성바이오로직스', 'KOSPI'),
('006400', '삼성SDI', 'KOSPI'),
('051910', 'LG화학', 'KOSPI'),
('028260', '삼성물산', 'KOSPI'),
('068270', '셀트리온', 'KOSPI');