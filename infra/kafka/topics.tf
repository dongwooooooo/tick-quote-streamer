# Kafka Topics 정의 (Terraform)

resource "kafka_topic" "quote_stream" {
  name               = "quote-stream"
  replication_factor = 3
  partitions         = 6
  
  config = {
    "cleanup.policy"                      = "delete"
    "delete.retention.ms"                 = "86400000"  # 1일
    "segment.ms"                          = "604800000" # 7일
    "retention.ms"                        = "604800000" # 7일
    "compression.type"                    = "snappy"
    "min.insync.replicas"                = "2"
  }
}

resource "kafka_topic" "orderbook_stream" {
  name               = "orderbook-stream"
  replication_factor = 3
  partitions         = 6
  
  config = {
    "cleanup.policy"                      = "delete"
    "delete.retention.ms"                 = "86400000"  # 1일
    "segment.ms"                          = "604800000" # 7일
    "retention.ms"                        = "604800000" # 7일
    "compression.type"                    = "snappy"
    "min.insync.replicas"                = "2"
  }
}

resource "kafka_topic" "notification_alerts" {
  name               = "notification-alerts"
  replication_factor = 3
  partitions         = 3
  
  config = {
    "cleanup.policy"                      = "delete"
    "delete.retention.ms"                 = "86400000"  # 1일
    "segment.ms"                          = "604800000" # 7일
    "retention.ms"                        = "604800000" # 7일
    "compression.type"                    = "snappy"
    "min.insync.replicas"                = "2"
  }
}