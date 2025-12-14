package com.example.demo.securesql.whitelist;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQL 함수 화이트리스트 관리 클래스
 *
 * - classpath:/FunctionWhitelist.properties 기본 로드
 * - ./config/FunctionWhitelist.properties 존재 시 override
 * - 외부 파일 변경 시 핫 리로드 가능
 */
@Slf4j
public class GlobalFunctionWhitelistRegistry_new {

    /** classpath 기본 리소스 */
    private static final String CLASSPATH_RESOURCE = "/FunctionWhitelist.properties";

    /** 외부 설정 파일 (현재 미사용) */
    // private static final Path EXTERNAL = Paths.get("config/FunctionWhitelist.properties");
    private static final Path EXTERNAL = null;

    /** 허용된 함수 목록 */
    private static final Set<String> FUNCTIONS = ConcurrentHashMap.newKeySet();

    /* ===============================
       static initialization
       =============================== */
    static {
        // 1️⃣ classpath 기본 로드
        loadFromClasspath();

        // 2️⃣ 외부 파일 로드 (존재할 때만)
        if (EXTERNAL != null && Files.exists(EXTERNAL)) {
            loadFromExternal(EXTERNAL);
        }

        // 3️⃣ 외부 파일 watch (존재할 때만)
        if (EXTERNAL != null && EXTERNAL.getParent() != null) {
            startWatch(EXTERNAL.getParent());
        }
    }

    /* ===============================
       load helpers
       =============================== */

    private static void clearAll() {
        FUNCTIONS.clear();
    }

    private static void loadFromClasspath() {
        try (InputStream in =
                 GlobalFunctionWhitelistRegistry_new.class
                     .getResourceAsStream(CLASSPATH_RESOURCE)) {

            if (in == null) {
                log.warn("[FunctionWhitelist] Classpath resource not found: {}", CLASSPATH_RESOURCE);
                return;
            }

            loadStream(in, false);

        } catch (Exception e) {
            throw new RuntimeException("Classpath function whitelist 로드 실패", e);
        }
    }

    private static void loadFromExternal(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            loadStream(in, true);
            log.info("[FunctionWhitelist] External file loaded: {}", path.toAbsolutePath());
        } catch (Exception e) {
            log.error("[FunctionWhitelist] External file load failed: {}", path.toAbsolutePath(), e);
        }
    }

    private static void loadStream(InputStream in, boolean overrideAll) throws Exception {

        if (overrideAll) {
            clearAll();
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                FUNCTIONS.add(line.toUpperCase());
            }
        }
    }

    /* ===============================
       watch service
       =============================== */

    private static void startWatch(Path dir) {
        Thread t = new Thread(() -> {
            try {
                WatchService ws = FileSystems.getDefault().newWatchService();
                dir.register(ws,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE);

                while (true) {
                    WatchKey key = ws.take();
                    for (WatchEvent<?> ev : key.pollEvents()) {
                        Path changed = (Path) ev.context();

                        if (changed != null
                                && EXTERNAL != null
                                && changed.endsWith(EXTERNAL.getFileName())) {

                            loadFromExternal(EXTERNAL);
                            log.info("[FunctionWhitelist] External whitelist reloaded.");
                        }
                    }
                    key.reset();
                }
            } catch (Exception e) {
                log.error("[FunctionWhitelist] File Watch Service terminated.", e);
            }
        }, "FunctionWhitelistReloadWatcher");

        t.setDaemon(true);
        t.start();
    }

    /* ===============================
       public API
       =============================== */

    public static boolean isAllowedFunction(String functionName) {
        if (functionName == null) return false;
        return FUNCTIONS.contains(functionName.toUpperCase());
    }

    public static Set<String> all() {
        return Collections.unmodifiableSet(FUNCTIONS);
    }
}
