package com.gxj.cropyield.modules.auth.service.impl;

import com.gxj.cropyield.common.exception.BusinessException;
import com.gxj.cropyield.common.response.ResultCode;
import com.gxj.cropyield.modules.auth.dto.CaptchaResponse;
import com.gxj.cropyield.modules.auth.service.CaptchaService;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final long EXPIRE_SECONDS = 120;

    private final Map<String, CaptchaHolder> captchaStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Override
    public CaptchaResponse createCaptcha() {
        cleanupExpired();
        Captcha captcha = generateCaptcha();
        String id = UUID.randomUUID().toString();
        captchaStore.put(id, new CaptchaHolder(captcha.answer(), Instant.now().plusSeconds(EXPIRE_SECONDS)));
        return new CaptchaResponse(id, createImageBase64(captcha.expression()));
    }

    @Override
    public void validate(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码不能为空");
        }
        CaptchaHolder holder = captchaStore.remove(captchaId);
        if (holder == null || holder.expired() || !holder.answer().equals(captchaCode.trim())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误或已过期");
        }
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        captchaStore.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private Captcha generateCaptcha() {
        int operation = random.nextInt(4);
        return switch (operation) {
            case 0 -> generateAddition();
            case 1 -> generateSubtraction();
            case 2 -> generateMultiplication();
            default -> generateDivision();
        };
    }

    private Captcha generateAddition() {
        int first = random.nextInt(11);
        int second = random.nextInt(11 - first);
        int result = first + second;
        return new Captcha(formatExpression(first, second, "+"), Integer.toString(result));
    }

    private Captcha generateSubtraction() {
        int first = random.nextInt(11);
        int second = random.nextInt(first + 1);
        int result = first - second;
        return new Captcha(formatExpression(first, second, "-"), Integer.toString(result));
    }

    private Captcha generateMultiplication() {
        int first = random.nextInt(11);
        int maxSecond = first == 0 ? 10 : 10 / first;
        int second = random.nextInt(maxSecond + 1);
        int result = first * second;
        return new Captcha(formatExpression(first, second, "×"), Integer.toString(result));
    }

    private Captcha generateDivision() {
        int divisor = random.nextInt(10) + 1;
        int maxQuotient = 10 / divisor;
        int quotient = random.nextInt(maxQuotient + 1);
        int dividend = divisor * quotient;
        return new Captcha(formatExpression(dividend, divisor, "÷"), Integer.toString(quotient));
    }

    private String formatExpression(int first, int second, String operator) {
        return first + " " + operator + " " + second + " = ?";
    }

    private String createImageBase64(String expression) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        for (int i = 0; i < 6; i++) {
            g2d.setColor(randomColor());
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g2d.drawLine(x1, y1, x2, y2);
        }

        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int x = (WIDTH - fontMetrics.stringWidth(expression)) / 2;
        int y = ((HEIGHT - fontMetrics.getHeight()) / 2) + fontMetrics.getAscent();

        int offset = 0;
        for (int i = 0; i < expression.length(); i++) {
            g2d.setColor(randomColor());
            char character = expression.charAt(i);
            g2d.drawString(String.valueOf(character), x + offset, y);
            offset += fontMetrics.charWidth(character);
        }

        g2d.dispose();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "验证码生成失败");
        }
    }

    private Color randomColor() {
        return new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200));
    }

    private record CaptchaHolder(String answer, Instant expiresAt) {
        private boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private record Captcha(String expression, String answer) {
    }
}
