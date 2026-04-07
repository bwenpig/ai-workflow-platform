package com.ben.workflow.executor;

import com.ben.dagscheduler.spi.NodeExecutionContext;
import com.ben.workflow.executor.extension.BaseExecutor;
import com.ben.workflow.executor.extension.ExecutorMeta;
import com.ben.workflow.executor.extension.ParameterSchema;
import com.ben.workflow.executor.extension.ValidationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.util.*;

/**
 * 邮件发送执行器
 * <p>
 * 通过 SMTP 发送邮件，支持 HTML 正文和附件。
 * <p>
 * config 参数说明：
 * <ul>
 *   <li>to (String/List, 必填) - 收件人</li>
 *   <li>cc (String/List, 可选) - 抄送</li>
 *   <li>subject (String, 必填) - 邮件主题</li>
 *   <li>body (String, 必填) - 邮件正文（支持 HTML）</li>
 *   <li>from (String, 可选) - 发件人</li>
 *   <li>attachments (List, 可选) - 附件路径列表</li>
 * </ul>
 */
@ExecutorMeta(
    type = "email",
    name = "Send Email",
    description = "Send email via SMTP",
    category = "integration",
    icon = "📧"
)
public class EmailExecutor extends BaseExecutor {

    private JavaMailSender mailSender;

    @Override
    public String getType() {
        return "email";
    }

    @Override
    public String getName() {
        return "Send Email";
    }

    @Override
    public String getDescription() {
        return "Send email via SMTP";
    }

    @Override
    protected void doInitialize() throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        if (configuration != null) {
            sender.setHost(configuration.getString("smtpHost", "localhost"));
            sender.setPort(configuration.getInt("smtpPort", 25));
            sender.setUsername(configuration.getString("username", ""));
            sender.setPassword(configuration.getString("password", ""));

            Properties props = sender.getJavaMailProperties();
            if (configuration.getBoolean("useTls", false)) {
                props.put("mail.smtp.starttls.enable", "true");
            }
            props.put("mail.smtp.auth", String.valueOf(
                !configuration.getString("username", "").isEmpty()
            ));
        }

        this.mailSender = sender;
    }

    @Override
    protected void doValidate(NodeExecutionContext context) throws ValidationException {
        super.doValidate(context);
        requireInput(context, "to", "subject", "body");
    }

    @Override
    protected Map<String, Object> doExecute(NodeExecutionContext context) throws Exception {
        List<String> to = resolveRecipients(context, "to");
        List<String> cc = resolveRecipients(context, "cc");
        String subject = getInputString(context, "subject", "");
        String body = getInputString(context, "body", "");
        String from = getInputString(context, "from", null);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to.toArray(new String[0]));
        if (!cc.isEmpty()) {
            helper.setCc(cc.toArray(new String[0]));
        }
        helper.setSubject(subject);
        helper.setText(body, true); // HTML
        if (from != null && !from.isBlank()) {
            helper.setFrom(from);
        }

        // 附件
        Object attachObj = getInput(context, "attachments");
        if (attachObj instanceof List) {
            for (Object path : (List<?>) attachObj) {
                if (path != null) {
                    File file = new File(path.toString());
                    if (file.exists()) {
                        helper.addAttachment(file.getName(), file);
                    }
                }
            }
        }

        mailSender.send(message);

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("sent", true);
        outputs.put("recipients", to);
        outputs.put("subject", subject);
        return outputs;
    }

    @Override
    protected List<ParameterSchema> defineInputParams() {
        return List.of(
            ParameterSchema.builder().name("to").type("array").label("To").description("Recipients").required(true).build(),
            ParameterSchema.builder().name("cc").type("array").label("CC").description("Carbon copy").required(false).build(),
            ParameterSchema.builder().name("subject").type("string").label("Subject").description("Email subject").required(true).build(),
            ParameterSchema.builder().name("body").type("string").label("Body").description("Email body (HTML)").required(true).build(),
            ParameterSchema.builder().name("from").type("string").label("From").description("Sender address").required(false).build(),
            ParameterSchema.builder().name("attachments").type("array").label("Attachments").description("File paths").required(false).build()
        );
    }

    @Override
    protected List<ParameterSchema> defineOutputParams() {
        return List.of(
            ParameterSchema.builder().name("sent").type("boolean").label("Sent").description("Whether email was sent").build(),
            ParameterSchema.builder().name("recipients").type("array").label("Recipients").description("List of recipients").build(),
            ParameterSchema.builder().name("subject").type("string").label("Subject").description("Email subject").build()
        );
    }

    /**
     * 解析收件人列表
     */
    @SuppressWarnings("unchecked")
    private List<String> resolveRecipients(NodeExecutionContext context, String key) {
        Object val = getInput(context, key);
        if (val == null) return Collections.emptyList();
        if (val instanceof List) {
            return ((List<Object>) val).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList();
        }
        if (val instanceof String) {
            String s = (String) val;
            if (s.contains(",")) {
                return Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(x -> !x.isEmpty())
                        .toList();
            }
            return List.of(s);
        }
        return Collections.emptyList();
    }

    // 测试用: 允许注入 mock mailSender
    void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
}
