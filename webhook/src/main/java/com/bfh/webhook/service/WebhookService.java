package com.bfh.webhook.service;

import com.bfh.webhook.dto.FinalQueryRequest;
import com.bfh.webhook.dto.WebhookRequest;
import com.bfh.webhook.dto.WebhookResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void executeFlow() {

        WebhookResponse response = generateWebhook();

        if (response == null ||
                response.getWebhook() == null ||
                response.getAccessToken() == null) {
            throw new RuntimeException("Failed to generate webhook or token");
        }

        String regNo = "Tanu179";  
        String finalSql = decideSql(regNo);

        submitFinalQuery(
                response.getWebhook(),
                response.getAccessToken(),
                finalSql
        );
    }

    private WebhookResponse generateWebhook() {

        String url =
                "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        WebhookRequest request = new WebhookRequest(
                "Tanuja Ghaytidak",          
                "Tanu179",           
                "ghaytidaktanuja@gmail.com"    
        );

        return restTemplate.postForObject(
                url,
                request,
                WebhookResponse.class
        );
    }

    private String decideSql(String regNo) {

        String digits = regNo.replaceAll("\\D", "");

        int lastDigit = digits.charAt(digits.length() - 1) - '0';

        // EVEN → QUESTION 2
        if (lastDigit % 2 == 0) {
            return """
            SELECT
                d.DEPARTMENT_NAME,
                AVG(TIMESTAMPDIFF(YEAR, e.DOB, CURDATE())) AS AVERAGE_AGE,
                GROUP_CONCAT(
                    CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME)
                    SEPARATOR ', '
                ) AS EMPLOYEE_LIST
            FROM DEPARTMENT d
            JOIN EMPLOYEE e
                ON d.DEPARTMENT_ID = e.DEPARTMENT
            JOIN PAYMENTS p
                ON e.EMP_ID = p.EMP_ID
            WHERE p.AMOUNT > 70000
            GROUP BY d.DEPARTMENT_ID, d.DEPARTMENT_NAME
            ORDER BY d.DEPARTMENT_ID DESC;
            """;
        }

        // ODD → QUESTION 1
        return """
        SELECT
            d.DEPARTMENT_NAME,
            p.AMOUNT AS SALARY,
            CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS EMPLOYEE_NAME,
            TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE
        FROM DEPARTMENT d
        JOIN EMPLOYEE e
            ON d.DEPARTMENT_ID = e.DEPARTMENT
        JOIN PAYMENTS p
            ON e.EMP_ID = p.EMP_ID
        WHERE DAY(p.PAYMENT_TIME) <> 1
        AND p.AMOUNT = (
            SELECT MAX(p2.AMOUNT)
            FROM PAYMENTS p2
            JOIN EMPLOYEE e2
                ON p2.EMP_ID = e2.EMP_ID
            WHERE e2.DEPARTMENT = d.DEPARTMENT_ID
            AND DAY(p2.PAYMENT_TIME) <> 1
        );
        """;
    }


    private void submitFinalQuery(String webhookUrl,
                                  String token,
                                  String sql) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", token);

        FinalQueryRequest body = new FinalQueryRequest(sql);

        HttpEntity<FinalQueryRequest> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        webhookUrl,
                        entity,
                        String.class
                );

        System.out.println("Submission response: " + response.getBody());
    }
}
