package com.example.demo.securesql.whitelist;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * 테이블/컬럼 화이트리스트 관리 클래스 (동적 핫-리로드).
 *
 * - classpath:/TableWhitelist.properties 기본 로드
 * - ./config/TableWhitelist.properties 존재하면 override
 * - ./config 디렉터리 WatchService 로 감시 (변경 시 자동 재로딩)
 *
 *  함수 화이트리스트는 별도의 GlobalFunctionWhitelistRegistry 에서 관리.
 */
@Slf4j
public class DynamicTableWhitelistRegistry_old {

	// 클래스패스 내 화이트리스트 파일 경로 상수
    private static final String CLASSPATH_RESOURCE = "/TableWhitelist.properties";
    // 외부 설정 파일 경로 상수 (config 디렉토리 내)
//    private static final Path EXTERNAL = Paths.get("config/TableWhitelist.properties");
    // 현재 외부파일을 사용하지 않기 때문에 null로 초기화. 차후 외부파일을 사용하게 되면 주석 풀고 경로 설정하면 됨.
    private static final Path EXTERNAL = null;

    // [테이블명 -> 컬럼명 Set] 구조의 화이트리스트 저장소 (동시성 환경 안전 보장)
    private static final Map<String, Set<String>> TABLE_COLUMNS = new ConcurrentHashMap<>();

    // 클래스 로딩 시 단 한 번 실행되는 정적 초기화 블록
    static {
    	// 클래스패스 리소스를 먼저 로드 (기본값 설정)
        loadFromClasspath();
        
        // 외부 설정 파일 존재 여부 확인
        if (Files.exists(EXTERNAL)) {
        	// 외부 파일 로드 (존재 시 기본값을 덮어쓸 수 있음)
            loadFromExternal(EXTERNAL);
        }
        
        // 외부 설정 파일의 부모 디렉토리가 존재하는지 확인
        if (EXTERNAL.getParent() != null) {
        	// 해당 디렉토리에 대해 파일 변경 감시(WatchService) 시작
            startWatch(EXTERNAL.getParent());
        }
    }

    /** 모든 화이트리스트 데이터를 지우는 헬퍼 메서드 **/
    private static void clearAll() {
        TABLE_COLUMNS.clear();
    }

    // 클래스패스에서 파일을 로드하는 메서드
    private static void loadFromClasspath() {
        try (InputStream in = DynamicTableWhitelistRegistry_old.class.getResourceAsStream(CLASSPATH_RESOURCE)) {
        	// 파일이 없으면 종료
            if (in == null) {
                return;
            }
            
            // 스트림을 읽어 데이터를 로드 (overrideAll=false, 기존 데이터 유지/병합)
            loadStream(in, false);
        } catch (Exception e) {
        	// 로드 실패 시 시스템 종료
            throw new RuntimeException("Classpath whitelist 로드 실패", e);
        }
    }

    /** 외부 파일 시스템에서 파일을 로드하는 메서드 **/
    private static void loadFromExternal(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
        	// 스트림을 읽어 데이터를 로드 (overrideAll=true, 기존 데이터 전체 덮어쓰기)
            loadStream(in, true);
            log.info("[TableWhitelist] External file loaded: {}", path.toAbsolutePath());
        } catch (Exception e) {
        	// 예외 발생 시 에러로그 출력 (로딩은 계속)
            log.error("[TableWhitelist] External file load failed: {}", path.toAbsolutePath(), e);
        }
    }

    /** 실제 파일/스트림을 읽어 Map에 데이터를 채우는 공통 로직 **/
    private static void loadStream(InputStream in, boolean overrideAll) throws Exception {
    	// 외부 파일 로드시
        if (overrideAll) {
        	// 기존 모든 데이터를 지움
            clearAll();
        }
        
        /*
         * Table, Column whitelist 작성 예시
         * 
         * table=SALES_TRANSACTION
    	 * columns=STORE_ID,PRODUCT_ID,TX_DATE,QTY,UNIT_PRICE,FINAL_AMOUNT
         */
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            // 현재 처리 중인 테이블명을 저장
            String currentTable = null;
            // 한 줄씩 읽기
            while ((line = br.readLine()) != null) {
            	// 앞뒤 공백 제거
                line = line.trim();
                // 공백 줄은 테이블 섹션의 끝으로 간주
                if (line.isEmpty()) {
                    currentTable = null;
                    continue;
                }
                
                // 주석 건너뛰기
                if (line.startsWith("#")) continue;

                // '=' 기준으로 키/값 분리
                String[] parts = line.split("=", 2);
                // 키=값 형태가 아니면 건너뛰기
                if (parts.length != 2) continue;
                String key = parts[0].trim();
                String val = parts[1].trim();

                // 테이블 정의 키워드일 경우
                if ("table".equalsIgnoreCase(key)) {
                	// 테이블명을 대문자로 저장
                    currentTable = val.toUpperCase();
                    // TABLE_COLUMNS에 테이블명이 없으면 빈 Set으로 초기화
                    TABLE_COLUMNS.putIfAbsent(currentTable, ConcurrentHashMap.newKeySet());
                } 
                // 컬럼 정의 키워드이고, 현재 테이블이 설정되어 있을 경우
                else if ("columns".equalsIgnoreCase(key) && currentTable != null) {
                    Set<String> cols = new HashSet<>();
                    // 콤마(,) 기준으로 컬럼 목록을 분리
                    for (String c : val.split(",")) {
                    	// 대문자로 변환 후 Set에 추가
                        if (!c.isBlank()) cols.add(c.trim().toUpperCase());
                    }
                    
                    // 현재 테이블에 대해 컬럼 목록을 덮어쓰거나 초기화
                    TABLE_COLUMNS.put(currentTable, cols);
                }
            }
        }
    }

    /** 외부 설정 파일 디렉토리를 감시하여 변경 시 자동 재로딩하는 쓰레드 시작 **/
    private static void startWatch(Path dir) {
    	// 새 쓰레드 생성
        Thread t = new Thread(() -> {
            try {
            	// WatchService 생성
                WatchService ws = FileSystems.getDefault().newWatchService();
                // 디렉토리에 파일 수정 및 생성 이벤트 감시 등록
                dir.register(ws,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE);
                
                // 무한 루프
                while (true) {
                	// 이벤트가 발생할 때까지 대기
                    WatchKey key = ws.take();
                    // 발생한 이벤트 순회
                    for (WatchEvent<?> ev : key.pollEvents()) {
                        Path changed = (Path) ev.context();
                        
                        // 변경된 파일이 EXTERNAL 파일명과 일치하는지 확인
                        if (changed != null && changed.endsWith(EXTERNAL.getFileName())) {
                        	// 일치하면 외부 설정 파일 재로딩
                            loadFromExternal(EXTERNAL);
                            log.info("[TableWhitelist] External whitelist reloaded.");
                        }
                    }
                    
                    // 키를 재설정하여 다음 이벤트를 받을 준비
                    key.reset();
                }
            } catch (Exception e) {
            	// 예외 발생 시 에러로그 출력
                log.error("[TableWhitelist] File Watch Service abruptly terminated.", e);
            }
        }, "TableWhitelistReloadWatcher"); // 쓰레드 이름 지정
        
        // 데몬 쓰레드로 설정 (주 프로그램 종료 시 자동 종료)
        t.setDaemon(true);  
        // 쓰레드 시작
        t.start();
    }

    /** 특정 테이블에 허용된 컬럼 목록을 반환 **/
    public static Set<String> getColumnsForTable(String table) {
        if (table == null) return Collections.emptySet();
        // 대문자 변환 후 검색
        return TABLE_COLUMNS.getOrDefault(table.toUpperCase(), Collections.emptySet());
    }

    /** 특정 테이블에서 특정 컬럼이 허용되는지 확인 **/
    public static boolean isAllowedColumn(String table, String col) {
        if (table == null || col == null) return false;
        // 해당 테이블의 Set에 컬럼이 포함되어 있는지 확인
        return getColumnsForTable(table).contains(col.toUpperCase());
    }
}
