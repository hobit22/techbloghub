# AWS Certificate Manager (ACM) SSL 인증서 설정
# teckbloghub.kr 도메인용 무료 SSL 인증서

# SSL 인증서 생성 (와일드카드 인증서)
resource "aws_acm_certificate" "techbloghub" {
  domain_name               = "teckbloghub.kr"
  subject_alternative_names = ["*.teckbloghub.kr"]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "TechBlogHub SSL Certificate"
  }
}

# DNS 검증을 위한 Route 53 레코드 생성
resource "aws_route53_record" "cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.techbloghub.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = aws_route53_zone.techbloghub.zone_id
}

# 인증서 검증 완료 대기
resource "aws_acm_certificate_validation" "techbloghub" {
  certificate_arn         = aws_acm_certificate.techbloghub.arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]

  timeouts {
    create = "10m"
  }
}

# Outputs
output "certificate_arn" {
  description = "SSL 인증서 ARN"
  value       = aws_acm_certificate_validation.techbloghub.certificate_arn
}

output "certificate_status" {
  description = "SSL 인증서 상태"
  value       = aws_acm_certificate.techbloghub.status
}