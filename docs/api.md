# HTTP API

The publisher listens on port `8081` by default.

## Submit an email

`POST /api/email/send`

This endpoint currently does not require dashboard authentication.

Request content type: `application/json`

| Field | Type | Required | Description |
|---|---|---:|---|
| `to` | string | Yes | Recipient email address. |
| `subject` | string | Yes | Email subject. |
| `htmlTemplate` | string | Yes | HTML body of the email. |

Example:

```sh
curl -i -X POST http://localhost:8081/api/email/send \
  -H 'Content-Type: application/json' \
  -d '{"to":"person@example.com","subject":"Welcome","htmlTemplate":"<h1>Hello</h1>"}'
```

| Response | Meaning |
|---|---|
| `202 Accepted` | The publisher accepted the request for queueing. This does not confirm SMTP delivery. |
| `400 Bad Request` | The request body could not be parsed. |
| `5xx` | The publisher could not process the request. |

## Dashboard and setup endpoints

| Method | Path | Access | Behavior |
|---|---|---|---|
| `GET` | `/` | Public | Redirects to first-run setup if there is no account; otherwise opens the dashboard, which requires login. |
| `GET` | `/setup` | Public | Opens first-run setup, or redirects to login if setup is complete. |
| `GET` | `/setup.html` | Public | Static first-run account form. |
| `POST` | `/setup` | Public, CSRF token required | Creates the first account. Returns `201`; invalid input returns `400`; an existing account returns `409`. Passwords must be 12–72 UTF-8 bytes. |
| `GET` | `/csrf` | Public | Returns the CSRF token and header name for browser form submissions. |
| `GET` | `/config.yml` | Login required | Returns saved non-secret configuration as JSON, or `204 No Content` if none is saved. SMTP password is never returned. |
| `POST` | `/config.yml` | Login required, CSRF token required | Saves settings and returns a downloadable `application.yml`. Invalid SMTP settings return `400`. |
| `GET` | `/login` | Public | Spring Security’s login page. |
| `POST` | `/login` | Public, CSRF token required | Form fields: `username` and `password`. Success redirects to `/index.html`; failed login redirects back to `/login?error`. |
| `GET` | `/index.html` | Login required | Dashboard configuration UI. |

Dashboard form endpoints use URL-encoded form data. The browser obtains a CSRF token from `GET /csrf` and sends it using the returned header name for setup and configuration changes.
