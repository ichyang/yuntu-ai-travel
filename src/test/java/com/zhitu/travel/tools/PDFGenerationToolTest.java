package com.zhitu.travel.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "云途智行出行计划.pdf";
        String content = "云途智行AI出行系统 - 智能旅行规划 https://github.com/ichyang/zhitu-ai-travel";
        String result = tool.generatePDF(fileName, content);
        assertNotNull(result);
    }
}