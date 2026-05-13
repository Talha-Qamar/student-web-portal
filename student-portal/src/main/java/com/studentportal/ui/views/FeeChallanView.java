package com.studentportal.ui.views;

import com.studentportal.dto.FeeChallanDto;
import com.studentportal.dto.FeeLineItemDto;
import com.studentportal.model.UserRole;
import com.studentportal.service.FeeChallanService;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;

@Route(value = "student/fees", layout = MainLayout.class)
@PageTitle("FAST Portal | Fee challan")
@AllowedRoles({UserRole.STUDENT})
public class FeeChallanView extends VerticalLayout {

    private final FeeChallanService feeChallanService;
    private final Grid<FeeLineItemDto> grid = new Grid<>(FeeLineItemDto.class, false);
    private final Div challanMeta = new Div();
    private final Div totalsCard = new Div();
    private final Span totalAmount = new Span();
    private final Span totalCredits = new Span();
    private FeeChallanDto cachedChallan;

    public FeeChallanView(FeeChallanService feeChallanService) {
        this.feeChallanService = feeChallanService;
        setSpacing(true);
        setPadding(true);
        addClassName("fee-view");

        add(new H2("Fee challan"));
        add(new Paragraph("Centralized view of tuition, surcharges, and activity dues for the current registration."));

        configureGrid();
        challanMeta.addClassName("challan-meta");
        totalsCard.addClassName("totals-card");
        totalsCard.add(new Paragraph("Total amount"));
        totalsCard.add(totalAmount);
        totalsCard.add(new Paragraph("Registered credit hours"));
        totalsCard.add(totalCredits);

        HorizontalLayout heroRow = new HorizontalLayout(challanMeta, totalsCard);
        heroRow.setWidthFull();
        heroRow.setAlignItems(Alignment.STRETCH);
        heroRow.setFlexGrow(2, challanMeta);
        heroRow.setFlexGrow(1, totalsCard);

        HorizontalLayout actions = buildActions();

        add(heroRow, grid, actions);
        refresh();
    }

    private void configureGrid() {
        grid.addColumn(FeeLineItemDto::getCode).setHeader("Code").setAutoWidth(true);
        grid.addColumn(FeeLineItemDto::getTitle).setHeader("Description").setFlexGrow(1);
        grid.addColumn(item -> item.getCreditHours() > 0 ? item.getCreditHours() + " CH" : "–")
                .setHeader("Credits")
                .setAutoWidth(true);
        grid.addColumn(item -> formatCurrency(item.getAmount()))
                .setHeader("Amount")
                .setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
    }

    private HorizontalLayout buildActions() {
        Button refreshButton = new Button("Refresh", event -> refresh());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button printButton = new Button("Print challan", event ->
                getUI().ifPresent(ui -> ui.getPage().executeJs("window.print()")));
        printButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(refreshButton, printButton);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return actions;
    }

    private void refresh() {
        try {
            Long studentId = SessionService.requireStudentId();
            cachedChallan = feeChallanService.generateChallan(studentId);
            renderChallan(cachedChallan);
        } catch (IllegalStateException ex) {
            Notification.show("Please sign in again to view your challan");
        }
    }

    private void renderChallan(FeeChallanDto challan) {
        if (challan == null) {
            grid.setItems(Collections.emptyList());
            challanMeta.removeAll();
            totalAmount.setText("Rs 0.00");
            totalCredits.setText("0 CH");
            return;
        }

        challanMeta.removeAll();
        challanMeta.add(new Span("Challan #" + challan.getChallanNumber()));
        challanMeta.add(new H3(challan.getStudentName()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        challanMeta.add(new Paragraph("Issued " + formatter.format(challan.getIssueDate())));
        challanMeta.add(new Paragraph("Due " + formatter.format(challan.getDueDate())));

        var items = challan.getItems() == null ? Collections.<FeeLineItemDto>emptyList() : challan.getItems();
        grid.setItems(items);
        totalAmount.setText(formatCurrency(challan.getTotalAmount()));
        totalCredits.setText(challan.getTotalCreditHours() + " CH");
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PK"));
        return formatter.format(amount);
    }
}
