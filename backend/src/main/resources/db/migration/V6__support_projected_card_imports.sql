-- Marco 2: distinguish effective card entries from future projections.

alter table organizadorfinanceiro.import_execution
    add column document_status varchar(20);

update organizadorfinanceiro.import_execution
set document_status = 'POSTED'
where target_type = 'CARD';

alter table organizadorfinanceiro.import_execution
    add constraint import_execution_document_status_check
        check (
            (target_type = 'CARD' and document_status in ('POSTED', 'PROJECTED'))
            or (target_type in ('ACCOUNT', 'PAYROLL') and document_status is null)
        );

alter table organizadorfinanceiro.financial_transaction
    add column posting_status varchar(20) not null default 'POSTED',
    add constraint financial_transaction_posting_status_check
        check (posting_status in ('POSTED', 'PROJECTED', 'CANCELLED'));

alter table organizadorfinanceiro.financial_transaction
    alter column posting_status drop default;

create index financial_transaction_projected_card_idx
    on organizadorfinanceiro.financial_transaction(user_id, credit_card_id, transaction_date)
    where posting_status = 'PROJECTED';

-- The immutable-row trigger does not need object lookup through search_path.
alter function organizadorfinanceiro.reject_immutable_change() set search_path = '';
