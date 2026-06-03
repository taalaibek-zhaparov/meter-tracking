# SSL Certificates

SSL certificates are not stored in the repository for security reasons.

## Generate self-signed certificate (local network)


mkdir -p infra/ssl
openssl req -x509 -nodes -days 730 -newkey rsa:2048 \
  -keyout infra/ssl/key.pem \
  -out infra/ssl/cert.pem \
  -subj "/CN=your_server_ip" \
  -addext "subjectAltName=IP:your_server_ip"


## Let's Encrypt (production with domain)


certbot certonly --standalone -d yourdomain.com

