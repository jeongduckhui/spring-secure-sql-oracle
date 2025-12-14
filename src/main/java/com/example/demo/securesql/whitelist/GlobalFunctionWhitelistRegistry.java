package com.example.demo.securesql.whitelist;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역 함수 화이트리스트 (NVL, DECODE, SUM, COUNT, CASE 등).
 *
 * - classpath:/FunctionWhitelist.properties 기본 로드
 * - ./config/FunctionWhitelist.properties 존재하면 override
 * - ./config 디렉터리 WatchService 로 감시
 */
@Slf4j
public class GlobalFunctionWhitelistRegistry {

	// 클래스패스 내부에 위치한 기본 화이트리스트 파일 경로
    private static final String CLASSPATH_RESOURCE = "/FunctionWhitelist.properties";
    // 외부 파일 시스템에 위치할 수 있는 오버라이드 화이트리스트 파일 경로
//    private static final Path EXTERNAL = Paths.get("config/FunctionWhitelist.properties");
    // 현재 외부파일을 사용하지 않기 때문에 null로 초기화. 차후 외부파일을 사용하게 되면 주석 풀고 경로 설정하면 됨.
    private static final Path EXTERNAL = null;

    // 허용된 함수 이름을 저장하는 Set. 동시성 환경에서 안전하게 사용하기 위해 ConcurrentHashMap.newKeySet() 사용
    private static final Set<String> FUNCTIONS = ConcurrentHashMap.newKeySet();

    // 클래스가 로딩될 때 (최초 한 번) 실행되는 정적 초기화 블록
    static {
    	// 클래스패스에서 기본 화이트리스트 로드
        loadFromClasspath();
        
        /*
        // 외부 파일 (./config/...)이 존재하는지 확인
        if (Files.exists(EXTERNAL)) {
        	// 존재하면 외부 파일 로드 (이때 기존 클래스패스 목록을 덮어씀)
            loadFromExternal(EXTERNAL);
        }
        
        // 외부 파일의 부모 디렉토리(config/)가 존재하는 경우
        if (EXTERNAL.getParent() != null) {
        	// 해당 디렉토리에 대해 파일 변경 감시(WatchService) 쓰레드 시작
            startWatch(EXTERNAL.getParent());
        }
        */
        
        if (EXTERNAL != null && Files.exists(EXTERNAL)) {
            loadFromExternal(EXTERNAL);
        }

        // 3️⃣ 외부 파일 watch (존재할 때만)
        if (EXTERNAL != null && EXTERNAL.getParent() != null) {
            startWatch(EXTERNAL.getParent());
        }
    }

    /** 현재 화이트리스트 Set을 완전히 비움 (보통 외부 파일 로드 시 덮어쓰기 전에 사용) **/
    private static void clearAll() {
        FUNCTIONS.clear();
    }

    /** 클래스패스 리소스에서 함수 화이트리스트를 로드 **/
    private static void loadFromClasspath() {
    	// ClassLoader를 통해 리소스 스트림을 얻음
        try (InputStream in = GlobalFunctionWhitelistRegistry.class.getResourceAsStream(CLASSPATH_RESOURCE)) {
        	// 리소스가 없는 경우 (null) 아무것도 하지 않고 종료
            if (in == null) {
                return;
            }
            
            // 스트림을 읽어 함수 목록에 추가 (덮어쓰기 하지 않음: false)
            loadStream(in, false);
        } catch (Exception e) {
        	// 로드 실패 시 RuntimeException 발생
            throw new RuntimeException("Classpath function whitelist 로드 실패", e);
        }
    }

    /** 외부 파일 시스템 경로에서 함수 화이트리스트를 로드 **/
    private static void loadFromExternal(Path path) {
    	// Files.newInputStream을 통해 파일 스트림을 얻음
        try (InputStream in = Files.newInputStream(path)) {
        	// 스트림을 읽어 함수 목록에 추가 (기존 목록 덮어쓰기: true)
            loadStream(in, true);
            log.info("[FuncWhitelist] External file loaded: {}", path.toAbsolutePath());
        } catch (Exception e) {
        	// 예외 발생 시 에러로그 출력 (로딩은 계속)
            log.error("[FuncWhitelist] External file load failed: {}", path.toAbsolutePath(), e);
        }
    }

    /** 입력 스트림에서 데이터를 읽어 FUNCTIONS Set에 추가 **/
    private static void loadStream(InputStream in, boolean overrideAll) throws Exception {
    	// 외부 파일 로드시 기존 목록을 덮어쓰기(Clear)
        if (overrideAll) {
            clearAll();
        }
        
        /*
         * 함수 whitelist 작성 예시
         * 
         * functions=NVL,DECODE,SUM,COUNT,AVG,MAX,MIN,CASE
         */
        // 스트림을 BufferedReader로 래핑하여 라인 단위로 읽음
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            
            // 파일의 끝(null)에 도달할 때까지 라인을 읽음
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // 빈 줄 무시
                if (line.isEmpty()) continue;
                // 주석(#) 줄 무시
                if (line.startsWith("#")) continue;

                // key=value 형태를 파싱하기 위해 '=' 기준으로 분리
                String[] parts = line.split("=", 2);
                // '='이 없거나 형식이 맞지 않으면 무시
                if (parts.length != 2) continue;
                String key = parts[0].trim();
                String val = parts[1].trim();

                // 키가 "functions"인 경우에만 처리
                if ("functions".equalsIgnoreCase(key)) {
                	// 값(val)을 쉼표(,)로 분리하여 각 함수 이름을 처리
                    for (String f : val.split(",")) {
                    	// 함수 이름이 공백이 아니면
                        if (!f.isBlank()) {
                        	// 앞뒤 공백 제거 및 대문자로 변환 후 Set에 추가
                            FUNCTIONS.add(f.trim().toUpperCase());
                        }
                    }
                }
            }
        }
    }

    /** 외부 설정 파일 디렉토리에 대한 파일 변경 감시(WatchService) 쓰레드를 시작 **/
    private static void startWatch(Path dir) {
    	// 새로운 쓰레드 생성
        Thread t = new Thread(() -> {
            try {
            	// WatchService 인스턴스 생성
                WatchService ws = FileSystems.getDefault().newWatchService();
                
                // 감시할 디렉토리(dir)에 파일 '수정' 및 '생성' 이벤트 등록
                dir.register(ws,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE);
                
                // 감시를 위한 무한 루프
                while (true) {
                	// 이벤트가 발생할 때까지 쓰레드 블록 (대기)
                    WatchKey key = ws.take();
                    
                    // 발생한 모든 이벤트 순회
                    for (WatchEvent<?> ev : key.pollEvents()) {
                    	// 변경된 파일의 상대 경로 이름 (context)을 얻음
                        Path changed = (Path) ev.context();
                        
                        // 변경된 파일 이름이 EXTERNAL 파일 이름과 일치하는지 확인
                        if (changed != null && changed.endsWith(EXTERNAL.getFileName())) {
                        	// 일치하면 외부 설정 파일 재로딩
                            loadFromExternal(EXTERNAL);
                            // 재로딩 성공 로그 출력
                            log.info("[FuncWhitelist] External function whitelist reloaded.");
                        }
                    }
                    
                    // WatchKey를 재설정하여 다음 이벤트를 받을 준비를 함
                    key.reset();
                }
            } catch (Exception e) {
            	// 예외 발생 시 에러로그 출력
                log.error("[FuncWhitelist] File Watch Service abruptly terminated.", e);
            }
        }, "FunctionWhitelistReloadWatcher"); // 쓰레드 이름 지정
        
        // 데몬 쓰레드로 설정 (주 프로그램 종료 시 자동 종료)
        t.setDaemon(true);
        // 쓰레드 시작
        t.start();
    }

    /** 주어진 함수 이름이 화이트리스트에 허용되었는지 확인 **/
    public static boolean isAllowedFunction(String funcName) {
        if (funcName == null) return false;
     // 대문자로 변환 후 Set에 포함되어 있는지 확인
        return FUNCTIONS.contains(funcName.toUpperCase());
    }

    /** 현재 등록된 함수 화이트리스트 Set을 읽기 전용으로 반환 **/
    public static Set<String> getFunctions() {
    	// 외부에서 Set을 수정할 수 없도록 unmodifiableSet으로 래핑하여 반환
        return Collections.unmodifiableSet(FUNCTIONS);
    }
}
