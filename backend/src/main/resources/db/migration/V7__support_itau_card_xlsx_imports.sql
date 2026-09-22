-- Marco 2: retain the status detected in Itaú workbooks.

alter table organizadorfinanceiro.import_execution
    drop constraint import_execution_document_status_check;

alter table organizadorfinanceiro.import_execution
    add constraint import_execution_document_status_check
        check (
            (target_type = 'CARD' and document_status in ('POSTED', 'PROJECTED', 'OPEN', 'PAID'))
            or (target_type in ('ACCOUNT', 'PAYROLL') and document_status is null)
        );
