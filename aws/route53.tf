# Route 53 도메인 설정
# 가비아에서 구매한 teckbloghub.kr 도메인을 AWS Route 53으로 관리

# Route 53 호스팅 영역 생성
resource "aws_route53_zone" "techbloghub" {
  name = "teckbloghub.kr"

  tags = {
    Name = "TechBlogHub Domain Zone"
  }
}

# 루트 도메인 A 레코드 (ALB 연결)
resource "aws_route53_record" "root" {
  zone_id = aws_route53_zone.techbloghub.zone_id
  name    = "teckbloghub.kr"
  type    = "A"

  alias {
    name                   = aws_lb.techbloghub.dns_name
    zone_id                = aws_lb.techbloghub.zone_id
    evaluate_target_health = true
  }
}

# www 서브도메인 A 레코드 (ALB 연결)
resource "aws_route53_record" "www" {
  zone_id = aws_route53_zone.techbloghub.zone_id
  name    = "www.teckbloghub.kr"
  type    = "A"

  alias {
    name                   = aws_lb.techbloghub.dns_name
    zone_id                = aws_lb.techbloghub.zone_id
    evaluate_target_health = true
  }
}

# API 서브도메인 A 레코드 (ALB 연결)
resource "aws_route53_record" "api" {
  zone_id = aws_route53_zone.techbloghub.zone_id
  name    = "api.teckbloghub.kr"
  type    = "A"

  alias {
    name                   = aws_lb.techbloghub.dns_name
    zone_id                = aws_lb.techbloghub.zone_id
    evaluate_target_health = true
  }
}

# Outputs
output "name_servers" {
  description = "가비아에서 네임서버로 설정해야 할 값들"
  value       = aws_route53_zone.techbloghub.name_servers
}

output "hosted_zone_id" {
  description = "Route 53 hosted zone ID"
  value       = aws_route53_zone.techbloghub.zone_id
}