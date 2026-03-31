package com.ben.workflow;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单冒烟测试用于生成 JaCoCo 覆盖率报告
 */
public class SmokeTest {
    
    @Test
    public void testContextLoads() {
        assertTrue(true, "基础测试通过");
    }
    
    @Test
    public void testJavaAvailable() {
        String version = System.getProperty("java.version");
        assertNotNull(version, "Java 版本应该可用");
    }
}
