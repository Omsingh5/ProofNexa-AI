package com.proofora.ai;

import com.proofora.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatUploadTest {
    @Test
    void readsPlainTextAndRejectsOtherTypes() {
        ChatUpload text = ChatUpload.parse("notes.txt", "Java and Spring".getBytes(StandardCharsets.UTF_8));
        assertThat(text.image()).isFalse();
        assertThat(text.text()).contains("Spring");

        assertThatThrownBy(() -> ChatUpload.parse("virus.exe", new byte[]{1, 2, 3}))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void readsDocxParagraphs() throws Exception {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(raw)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document><w:p><w:r><w:t>Backend engineer</w:t></w:r></w:p></w:document>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        ChatUpload doc = ChatUpload.parse("role.docx", raw.toByteArray());
        assertThat(doc.text()).contains("Backend engineer");
    }
}
