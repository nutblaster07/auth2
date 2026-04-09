package com.abhishek.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class EmailService {

    private final SendGrid sendGrid;
    private final String fromAddress;
    private final String fromName;
    private final int otpExpiryMinutes;

    public EmailService(
            SendGrid sendGrid,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.mail.from-name}") String fromName,
            @Value("${app.otp.expiryMinutes:10}") int otpExpiryMinutes
    ) {
        this.sendGrid = sendGrid;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    public void sendOtp(String toEmail, String otp, String purpose) {
        System.out.println("SENDGRID KEY USED: " + sendGrid);
        System.out.println("📧 OTP for " + toEmail + " is: " + otp);
        try {
            String subject = purpose.equals("signup")
                    ? "Verify your email - " + fromName
                    : "Your login OTP - " + fromName;

            Mail mail = new Mail(
                    new Email(fromAddress, fromName),
                    subject,
                    new Email(toEmail),
                    new Content("text/html", buildEmailHtml(otp, purpose))
            );

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                log.error("SendGrid rejected request for {} — status: {}, body: {}",
                        toEmail, response.getStatusCode(), response.getBody());
                throw new RuntimeException(
                        "SendGrid rejected email request with status " + response.getStatusCode()
                );
            }

            log.info("OTP email sent successfully to {} for purpose '{}'", toEmail, purpose);

        } catch (IOException e) {
            log.error("IOException while sending email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email. Please try again.");
        }
    }

    private String buildEmailHtml(String otp, String purpose) {
        String actionText = purpose.equals("signup")
                ? "verify your email address"
                : "log in to your account";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>OTP Verification</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="background:#f4f4f4;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="500" cellpadding="0" cellspacing="0"
                               style="background:#ffffff;border-radius:8px;overflow:hidden;
                                      box-shadow:0 2px 8px rgba(0,0,0,0.1);">

                          <!-- Header -->
                          <tr>
                            <td style="background:#1a1a2e;padding:30px;text-align:center;">
                              <h1 style="color:#ffffff;margin:0;font-size:24px;font-weight:600;">
                                %s
                              </h1>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:40px 30px;">
                              <p style="color:#333333;font-size:16px;margin:0 0 20px;">
                                Use the code below to %s:
                              </p>

                              <div style="background:#f8f9fa;border:2px solid #e9ecef;
                                          border-radius:8px;padding:20px;text-align:center;
                                          margin:20px 0;">
                                <span style="font-size:36px;font-weight:700;
                                             letter-spacing:8px;color:#1a1a2e;">
                                  %s
                                </span>
                              </div>

                              <p style="color:#666666;font-size:14px;margin:20px 0 0;">
                                This code expires in <strong>%d minutes</strong>.
                              </p>
                              <p style="color:#666666;font-size:14px;margin:10px 0 0;">
                                If you did not request this, please ignore this email.
                                Do not share this code with anyone.
                              </p>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="background:#f8f9fa;padding:20px 30px;text-align:center;
                                       border-top:1px solid #e9ecef;">
                              <p style="color:#999999;font-size:12px;margin:0;">
                                Sent by %s &nbsp;|&nbsp; %s
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(fromName, actionText, otp, otpExpiryMinutes, fromName, fromAddress);
    }
}