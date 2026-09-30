package com.employeemanagement.backend.service;

import com.employeemanagement.backend.entity.Employee;
import com.employeemanagement.backend.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ContractService {

    private static final float MARGIN = 56;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final NumberFormat NUMBER_FORMATTER = NumberFormat.getNumberInstance(Locale.US);

    private final EmployeeRepository employeeRepository;

    public ContractService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generateEmployeeContract(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + employeeId));

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                String fullName = employee.getFirstName() + " " + employee.getLastName();
                String department = valueOrNotSpecified(employee.getDepartment());
                String phoneNumber = valueOrNotSpecified(employee.getPhoneNumber());
                String formattedHireDate = formatDate(employee.getHireDate());
                String formattedSalary = formatSalary(employee.getSalary());
                String generatedDate = formatDate(LocalDate.now());

                float y = drawHeader(contentStream, regularFont, boldFont);

                y = drawWrappedText(
                        contentStream,
                        regularFont,
                        10.5f,
                        MARGIN,
                        y,
                        CONTENT_WIDTH,
                        15,
                        "This Employment Contract is generated based on the employee information recorded in the Employee Management system."
                ) - 6;
                y = drawWrappedText(
                        contentStream,
                        regularFont,
                        10.5f,
                        MARGIN,
                        y,
                        CONTENT_WIDTH,
                        15,
                        "This agreement is made between the employer and the employee identified below."
                ) - 18;

                y = drawSectionTitle(contentStream, boldFont, y, "EMPLOYEE INFORMATION");
                y = drawLabelValue(contentStream, boldFont, regularFont, y, "Full Name", fullName);
                y = drawLabelValue(contentStream, boldFont, regularFont, y, "Job Title", employee.getJobTitle());
                y = drawLabelValue(contentStream, boldFont, regularFont, y, "Department", department);
                y = drawLabelValue(contentStream, boldFont, regularFont, y, "Hire Date", formattedHireDate);
                y = drawLabelValue(contentStream, boldFont, regularFont, y, "Salary", formattedSalary);
                y = drawLabelValue(contentStream, boldFont, regularFont, y, "Email", employee.getEmail());
                y = drawLabelValue(contentStream, boldFont, regularFont, y, "Phone Number", phoneNumber) - 12;

                y = drawSectionTitle(contentStream, boldFont, y, "EMPLOYMENT TERMS");
                y = drawWrappedText(
                        contentStream,
                        regularFont,
                        10.5f,
                        MARGIN,
                        y,
                        CONTENT_WIDTH,
                        15,
                        "The employee is engaged in the position of " + employee.getJobTitle() + "."
                ) - 4;

                y = drawWrappedText(
                        contentStream,
                        regularFont,
                        10.5f,
                        MARGIN,
                        y,
                        CONTENT_WIDTH,
                        15,
                        "The employee's department is " + department + "."
                ) - 4;

                y = drawWrappedText(
                        contentStream,
                        regularFont,
                        10.5f,
                        MARGIN,
                        y,
                        CONTENT_WIDTH,
                        15,
                        "The employment start date is " + formattedHireDate + "."
                ) - 4;

                if (employee.getSalary() != null) {
                    y = drawWrappedText(
                            contentStream,
                            regularFont,
                            10.5f,
                            MARGIN,
                            y,
                            CONTENT_WIDTH,
                            15,
                            "The recorded salary for this employee is " + formattedSalary + "."
                    ) - 4;
                }

                y -= 14;
                y = drawSectionTitle(contentStream, boldFont, y, "ACKNOWLEDGEMENT");
                drawWrappedText(
                        contentStream,
                        regularFont,
                        10.5f,
                        MARGIN,
                        y,
                        CONTENT_WIDTH,
                        15,
                        "This document reflects the employment information recorded in the application at the time of generation."
                );

                drawSignatureArea(contentStream, regularFont, boldFont);
                drawFooter(contentStream, regularFont, generatedDate);
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to generate employee contract",
                    exception
            );
        }
    }

    private float drawHeader(PDPageContentStream contentStream, PDType1Font regularFont, PDType1Font boldFont)
            throws IOException {
        float y = PAGE_HEIGHT - MARGIN;

        drawText(contentStream, boldFont, 12, MARGIN, y, "EMPLOYEE MANAGEMENT");
        y -= 24;
        drawText(contentStream, boldFont, 22, MARGIN, y, "EMPLOYMENT CONTRACT");
        y -= 16;
        drawText(contentStream, regularFont, 8.5f, MARGIN, y, "Contract generated automatically");
        y -= 14;
        drawDivider(contentStream, y);

        return y - 26;
    }

    private float drawSectionTitle(PDPageContentStream contentStream, PDType1Font boldFont, float y, String title)
            throws IOException {
        drawText(contentStream, boldFont, 11, MARGIN, y, title);
        drawDivider(contentStream, y - 6);
        return y - 22;
    }

    private float drawLabelValue(
            PDPageContentStream contentStream,
            PDType1Font labelFont,
            PDType1Font valueFont,
            float y,
            String label,
            String value
    ) throws IOException {
        float labelX = MARGIN;
        float valueX = MARGIN + 128;
        float valueWidth = PAGE_WIDTH - MARGIN - valueX;

        drawText(contentStream, labelFont, 9.5f, labelX, y, label);
        return drawWrappedText(contentStream, valueFont, 9.5f, valueX, y, valueWidth, 13, value) - 5;
    }

    private float drawWrappedText(
            PDPageContentStream contentStream,
            PDType1Font font,
            float fontSize,
            float x,
            float y,
            float maxWidth,
            float lineSpacing,
            String text
    ) throws IOException {
        for (String line : wrapText(text, font, fontSize, maxWidth)) {
            drawText(contentStream, font, fontSize, x, y, line);
            y -= lineSpacing;
        }

        return y;
    }

    private void drawSignatureArea(PDPageContentStream contentStream, PDType1Font regularFont, PDType1Font boldFont)
            throws IOException {
        float signatureY = 170;
        float columnWidth = 220;
        float rightColumnX = MARGIN + 270;

        drawText(contentStream, boldFont, 10, MARGIN, signatureY, "Employer Signature");
        drawText(contentStream, regularFont, 10, MARGIN, signatureY - 34, "____________________________");
        drawText(contentStream, regularFont, 10, MARGIN, signatureY - 58, "Date: ______________________");

        drawText(contentStream, boldFont, 10, rightColumnX, signatureY, "Employee Signature");
        drawText(contentStream, regularFont, 10, rightColumnX, signatureY - 34, "____________________________");
        drawText(contentStream, regularFont, 10, rightColumnX, signatureY - 58, "Date: ______________________");
    }

    private void drawFooter(PDPageContentStream contentStream, PDType1Font regularFont, String generatedDate)
            throws IOException {
        float footerY = 48;

        drawDivider(contentStream, footerY + 18);
        drawText(contentStream, regularFont, 8.5f, MARGIN, footerY, "Generated by Employee Management");
        drawText(contentStream, regularFont, 8.5f, PAGE_WIDTH - MARGIN - 150, footerY,
                "Generated on " + generatedDate);
    }

    private void drawText(PDPageContentStream contentStream, PDType1Font font, float fontSize,
                          float x, float y, String text) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(sanitizeText(text));
        contentStream.endText();
    }

    private void drawDivider(PDPageContentStream contentStream, float y) throws IOException {
        contentStream.moveTo(MARGIN, y);
        contentStream.lineTo(PAGE_WIDTH - MARGIN, y);
        contentStream.setLineWidth(0.6f);
        contentStream.stroke();
    }

    private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = sanitizeText(text).split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;

            if (textWidth(candidate, font, fontSize) <= maxWidth) {
                currentLine = new StringBuilder(candidate);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                }

                currentLine = new StringBuilder(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private float textWidth(String text, PDType1Font font, float fontSize) throws IOException {
        return font.getStringWidth(sanitizeText(text)) / 1000 * fontSize;
    }

    private String valueOrNotSpecified(String value) {
        if (value == null || value.isBlank()) {
            return "Not specified";
        }

        return value;
    }

    private String formatSalary(BigDecimal salary) {
        if (salary == null) {
            return "Not specified";
        }

        return NUMBER_FORMATTER.format(salary);
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private String sanitizeText(String text) {
        if (text == null) {
            return "";
        }

        return text.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s{2,}", " ").trim();
    }
}
