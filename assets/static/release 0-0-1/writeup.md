# Pongpin is an email delivery service

- Dashboard: Create a login, configure SMTP and the queue provider, and generate the worker’s application.yml. Dashboard settings are encrypted locally; the login password is stored as a BCrypt hash.

- Email API: POST /api/email/send accepts a recipient, subject, and HTML body, then publishes the event to RabbitMQ.

- Architecture: The publisher routes events through a queue adapter; the worker selects its configured adapter and handles delivery. RabbitMQ is currently supported;

<img width="3300" height="1261" alt="architecture" src="https://github.com/user-attachments/assets/cd6cc730-b7e1-4e9f-8fd7-a341d6757afa" />

>Kafka and SQS are planned.
> Per-event priority routing is planned for a later release.
