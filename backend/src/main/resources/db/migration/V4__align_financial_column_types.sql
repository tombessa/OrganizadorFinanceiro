alter table organizadorfinanceiro.financial_account
    alter column currency type varchar(3);

alter table organizadorfinanceiro.credit_card
    alter column last_four_digits type varchar(4);

alter table organizadorfinanceiro.import_file
    alter column content_hash type varchar(64);

alter table organizadorfinanceiro.raw_transaction
    alter column raw_fingerprint type varchar(64);

alter table organizadorfinanceiro.financial_transaction
    alter column currency type varchar(3),
    alter column transaction_fingerprint type varchar(64);
