#!/usr/bin/env bash
set -uo pipefail

# Cloud Quiz API 集成测试脚本
# 依赖: curl, jq (可选但推荐)
# 用法: chmod +x scripts/test_api.sh && ./scripts/test_api.sh

# 统一脚本运行环境的 UTF-8，减少中文乱码（兼容 set -u 未设置变量）
if [[ -z "${LANG-}" || "${LANG-}" != *"UTF-8"* ]]; then
  export LANG=C.UTF-8
fi
if [[ -z "${LC_ALL-}" || "${LC_ALL-}" != *"UTF-8"* ]]; then
  export LC_ALL=C.UTF-8
fi

# 基础变量（使用默认值，避免 set -u 导致未绑定变量报错）
BASE_URL=${BASE_URL:-"http://localhost:8090"}
USER_NAME=${USER_NAME:-"test"}
USER_PASSWORD=${USER_PASSWORD:-"123456"}
ADMIN_USER=${ADMIN_USER:-"admin"}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-"123456"}
USER_TOKEN=${USER_TOKEN:-""}
ADMIN_TOKEN=${ADMIN_TOKEN:-""}
FAIL_COUNT=${FAIL_COUNT:-0}

# 工具/辅助函数
has_jq() { command -v jq >/dev/null 2>&1; }

print_title() {
  echo
  echo "==== $1 ===="
}

extract_json_field() {
  local json="$1" key="$2"
  printf '%s' "$json" | sed -n "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p"
}

parse_code() {
  local json="$1"
  if has_jq; then
    echo "$json" | jq -r '.code // empty'
  else
    printf '%s' "$json" | sed -n 's/.*"code"[[:space:]]*:[[:space:]]*\([-0-9]\+\).*/\1/p'
  fi
}

# RabbitMQ 管理 API JSON 字段提取（message_stats 下整数值）
get_rmq_stat() {
  local json="$1" key="$2" val
  if has_jq; then
    val=$(echo "$json" | jq -r ".message_stats.${key} // 0")
  else
    val=$(printf '%s' "$json" | sed -n "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p")
    [[ -z "$val" ]] && val=0
  fi
  printf '%s' "$val"
}

curl_json() {
  local method=$1 path=$2 token=$3 body=${4:-}
  if [[ -n "$body" ]]; then
    curl -sS -X "$method" "${BASE_URL}${path}" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json" \
      --data "$body"
  else
    curl -sS -X "$method" "${BASE_URL}${path}" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json"
  fi
}

assert_ok_code() {
  local json=$1
  local code
  code=$(parse_code "$json")
  if [[ -z "$code" ]]; then
    if echo "$json" | grep -qi 'token expired\|error'; then
      echo "ERROR: 未能解析 code，且检测到错误关键词" >&2
      echo "$json"
      FAIL_COUNT=$((FAIL_COUNT+1))
      return 1
    else
      echo "提示: 未解析到 code 字段，跳过严格断言。"
      return 0
    fi
  fi
  if [[ "$code" != "0" && "$code" != "200" ]]; then
    echo "ERROR: code=$code" >&2
    echo "$json" | (has_jq && jq . || cat)
    FAIL_COUNT=$((FAIL_COUNT+1))
    return 1
  fi
  return 0
}

wait_gateway_up() {
  print_title "检查网关健康"
  for i in {1..60}; do
    if curl -s "${BASE_URL}/actuator/health" | grep -q '"status":"UP"'; then
      echo "网关已就绪: ${BASE_URL}"
      return 0
    fi
    sleep 2
  done
  echo "等待网关超时，请确认容器已启动。" >&2
  exit 1
}

login_and_get_token() {
  local user="$1" pass="$2"
  local login_body
  login_body=$(cat <<EOF
{"userName":"${user}","password":"${pass}"}
EOF
)
  local resp
  resp=$(curl -sS -X POST "${BASE_URL}/api/user/login" -H "Content-Type: application/json" --data "$login_body")
  echo "$resp" | (has_jq && jq . || cat) >&2
  local code
  code=$(parse_code "$resp")
  if [[ "$code" != "0" && "$code" != "200" ]]; then
    echo "登录失败：code=$code" >&2
    FAIL_COUNT=$((FAIL_COUNT+1))
    echo "提示：请检查账号、密码或服务状态。" >&2
    return 1
  fi
  local token=""
  if has_jq; then
    token=$(echo "$resp" | jq -r '.data.token // empty')
  else
    token=$(extract_json_field "$resp" token)
  fi
  if [[ -z "$token" ]]; then
    echo "ERROR: 登录响应中未提取到 token" >&2
    FAIL_COUNT=$((FAIL_COUNT+1))
    return 1
  fi
  printf '%s' "$token"
}
# ====================== 负载均衡测试 ======================
test_load_balancing() {
  local token="$1"
  local request_count=${2:-10}

  print_title "负载均衡测试（按响应端口统计）"
  echo "将发送 ${request_count} 次请求以统计实例端口命中分布"
  echo

  # 使用临时目录存储头部与统计
  local tmp_dir="./.tmp_lb_$$"
  mkdir -p "$tmp_dir"

  # ---- user-service ----
  echo "--- 测试 user-service ---"
  local user_success=0
  : > "$tmp_dir/ports_user.txt"
  for i in $(seq 1 $request_count); do
    local hdr_file body_file inst resp_code
    hdr_file="$tmp_dir/h_user_$i.txt"
    body_file="$tmp_dir/b_user_$i.json"
    curl -sS -X GET "${BASE_URL}/api/user/info" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json" \
      -D "$hdr_file" -o "$body_file" 2>/dev/null

    # 成功判断
    if has_jq; then
      resp_code=$(jq -r '.code // empty' "$body_file" 2>/dev/null)
    else
      resp_code=$(sed -n 's/.*"code"[[:space:]]*:[[:space:]]*\([-0-9]\+\).*/\1/p' "$body_file")
    fi
    if [[ "$resp_code" == "0" || "$resp_code" == "200" ]]; then
      user_success=$((user_success + 1))
    fi

    # 实例识别（优先 X-Instance-Host，其次从 X-Instance-Id 提取 IP；最后回退端口）
    inst=$(grep -i '^X-Instance-Host:' "$hdr_file" | awk -F ': ' '{print $2}' | tr -d '\r')
    if [[ -z "$inst" ]]; then
      inst=$(grep -i '^X-Instance-Id:' "$hdr_file" | awk -F ': ' '{print $2}' | awk -F ':' '{print $1}' | tr -d '\r')
    fi
    if [[ -z "$inst" ]]; then
      inst=$(grep -i '^X-Instance-Port:' "$hdr_file" | awk -F ': ' '{print $2}' | tr -d '\r')
    fi
    [[ -n "$inst" ]] && echo "$inst" >> "$tmp_dir/ports_user.txt"
    sleep 0.05
  done
  echo "  成功: ${user_success}/${request_count}"
  if [[ -s "$tmp_dir/ports_user.txt" ]]; then
    echo "  实例分布（IP 或标识）:"
    awk '{c[$0]++} END{for (k in c) printf "    - 实例 %s: %d 次\n", k, c[k]}' "$tmp_dir/ports_user.txt"
  else
    echo "  未能提取到实例头部（X-Instance-Host / X-Instance-Id / X-Instance-Port）。"
  fi
  echo

  # ---- question-service ----
  echo "--- 测试 question-service ---"
  local question_success=0
  : > "$tmp_dir/ports_question.txt"
  for i in $(seq 1 $request_count); do
    local hdr_file body_file inst resp_code
    hdr_file="$tmp_dir/h_question_$i.txt"
    body_file="$tmp_dir/b_question_$i.json"
    curl -sS -X GET "${BASE_URL}/api/quiz/questions" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json" \
      -D "$hdr_file" -o "$body_file" 2>/dev/null

    if has_jq; then
      resp_code=$(jq -r '.code // empty' "$body_file" 2>/dev/null)
    else
      resp_code=$(sed -n 's/.*"code"[[:space:]]*:[[:space:]]*\([-0-9]\+\).*/\1/p' "$body_file")
    fi
    if [[ "$resp_code" == "0" || "$resp_code" == "200" ]]; then
      question_success=$((question_success + 1))
    fi

    inst=$(grep -i '^X-Instance-Host:' "$hdr_file" | awk -F ': ' '{print $2}' | tr -d '\r')
    if [[ -z "$inst" ]]; then
      inst=$(grep -i '^X-Instance-Id:' "$hdr_file" | awk -F ': ' '{print $2}' | awk -F ':' '{print $1}' | tr -d '\r')
    fi
    if [[ -z "$inst" ]]; then
      inst=$(grep -i '^X-Instance-Port:' "$hdr_file" | awk -F ': ' '{print $2}' | tr -d '\r')
    fi
    [[ -n "$inst" ]] && echo "$inst" >> "$tmp_dir/ports_question.txt"
    sleep 0.05
  done
  echo "  成功: ${question_success}/${request_count}"
  if [[ -s "$tmp_dir/ports_question.txt" ]]; then
    echo "  实例分布（IP 或标识）:"
    awk '{c[$0]++} END{for (k in c) printf "    - 实例 %s: %d 次\n", k, c[k]}' "$tmp_dir/ports_question.txt"
  else
    echo "  未能提取到实例头部（X-Instance-Host / X-Instance-Id / X-Instance-Port）。"
  fi
  echo

  # ---- answer-service（单实例） ----
  echo "--- 测试 answer-service ---"
  local answer_success=0
  : > "$tmp_dir/ports_answer.txt"
  for i in $(seq 1 5); do
    local hdr_file body_file inst resp_code
    hdr_file="$tmp_dir/h_answer_$i.txt"
    body_file="$tmp_dir/b_answer_$i.json"
    curl -sS -X GET "${BASE_URL}/api/answer/history/my" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json" \
      -D "$hdr_file" -o "$body_file" 2>/dev/null
    if has_jq; then
      resp_code=$(jq -r '.code // empty' "$body_file" 2>/dev/null)
    else
      resp_code=$(sed -n 's/.*"code"[[:space:]]*:[[:space:]]*\([-0-9]\+\).*/\1/p' "$body_file")
    fi
    if [[ "$resp_code" == "0" || "$resp_code" == "200" ]]; then
      answer_success=$((answer_success + 1))
    fi
    inst=$(grep -i '^X-Instance-Host:' "$hdr_file" | awk -F ': ' '{print $2}' | tr -d '\r')
    if [[ -z "$inst" ]]; then
      inst=$(grep -i '^X-Instance-Id:' "$hdr_file" | awk -F ': ' '{print $2}' | awk -F ':' '{print $1}' | tr -d '\r')
    fi
    if [[ -z "$inst" ]]; then
      inst=$(grep -i '^X-Instance-Port:' "$hdr_file" | awk -F ': ' '{print $2}' | tr -d '\r')
    fi
    [[ -n "$inst" ]] && echo "$inst" >> "$tmp_dir/ports_answer.txt"
    sleep 0.05
  done
  echo "  成功: ${answer_success}/5"
  if [[ -s "$tmp_dir/ports_answer.txt" ]]; then
    echo "  实例分布（IP 或标识）:"
    awk '{c[$0]++} END{for (k in c) printf "    - 实例 %s: %d 次\n", k, c[k]}' "$tmp_dir/ports_answer.txt"
  fi

  # 清理临时目录
  rm -rf "$tmp_dir"
  echo
}
#     else
#       user_fail=$((user_fail + 1))
#       printf "  请求 %2d: ✗ 失败\n" "$i"
#     fi
#     sleep 0.1
#   done
#   echo "  user-service 成功: ${user_success}/${request_count}, 失败: ${user_fail}"
#   echo

#   # 从 Docker 日志中统计实例处理情况
#   echo "  从日志中分析实例分布..."
#   local user1_count=0
#   local user2_count=0
  
#   # 检查 user-service-1 的日志
#   if docker logs user-service-1 --since "${start_time}s" 2>/dev/null | grep -q "/api/user/info"; then
#     user1_count=$(docker logs user-service-1 --since "${start_time}s" 2>&1 | grep -c "/api/user/info" || echo "0")
#   fi
  
#   # 检查 user-service-2 的日志
#   if docker logs user-service-2 --since "${start_time}s" 2>/dev/null | grep -q "/api/user/info"; then
#     user2_count=$(docker logs user-service-2 --since "${start_time}s" 2>&1 | grep -c "/api/user/info" || echo "0")
#   fi
  
#   echo "  实例日志分布:"
#   printf "    - user-service-1: %d 次请求\n" "$user1_count"
#   printf "    - user-service-2: %d 次请求\n" "$user2_count"
#   echo

#   # 测试 question-service 负载均衡
#   echo "--- 测试 question-service 负载均衡 (2个实例) ---"
#   local question_success=0
#   local question_fail=0
#   local question_start_time=$(date +%s)
  
#   for i in $(seq 1 $request_count); do
#     local resp
#     resp=$(curl -sS -X GET "${BASE_URL}/api/quiz/questions" \
#       -H "Authorization: Bearer ${token}" \
#       -H "Content-Type: application/json" 2>/dev/null)
    
#     if echo "$resp" | grep -q '"code"[[:space:]]*:[[:space:]]*[02]'; then
#       question_success=$((question_success + 1))
#       printf "  请求 %2d: ✓\n" "$i"
#     else
#       question_fail=$((question_fail + 1))
#       printf "  请求 %2d: ✗ 失败\n" "$i"
#     fi
#     sleep 0.1
#   done
#   echo "  question-service 成功: ${question_success}/${request_count}, 失败: ${question_fail}"
#   echo

#   # 从 Docker 日志中统计 question-service 实例处理情况
#   echo "  从日志中分析实例分布..."
#   local question1_count=0
#   local question2_count=0
  
#   if docker logs question-service-1 --since "${question_start_time}s" 2>/dev/null | grep -q "/api/quiz/questions"; then
#     question1_count=$(docker logs question-service-1 --since "${question_start_time}s" 2>&1 | grep -c "/api/quiz/questions" || echo "0")
#   fi
  
#   if docker logs question-service-2 --since "${question_start_time}s" 2>/dev/null | grep -q "/api/quiz/questions"; then
#     question2_count=$(docker logs question-service-2 --since "${question_start_time}s" 2>&1 | grep -c "/api/quiz/questions" || echo "0")
#   fi
  
#   echo "  实例日志分布:"
#   printf "    - question-service-1: %d 次请求\n" "$question1_count"
#   printf "    - question-service-2: %d 次请求\n" "$question2_count"
#   echo

#   # 测试 answer-service (1个实例)
#   echo "--- 测试 answer-service (1个实例) ---"
#   local answer_success=0
#   local answer_start_time=$(date +%s)
  
#   for i in $(seq 1 5); do
#     local resp
#     resp=$(curl -sS -X GET "${BASE_URL}/api/answer/history/my" \
#       -H "Authorization: Bearer ${token}" \
#       -H "Content-Type: application/json" 2>/dev/null)
    
#     if echo "$resp" | grep -q '"code"[[:space:]]*:[[:space:]]*[02]'; then
#       answer_success=$((answer_success + 1))
#       printf "  请求 %d: ✓\n" "$i"
#     else
#       printf "  请求 %d: ✗ 失败\n" "$i"
#     fi
#     sleep 0.1
#   done
#   echo "  answer-service 成功: ${answer_success}/5"
  
#   # 从 Docker 日志中统计 answer-service 实例处理情况
#   local answer_count=0
#   if docker logs answer-service --since "${answer_start_time}s" 2>/dev/null | grep -q "/api/answer/history/my"; then
#     answer_count=$(docker logs answer-service --since "${answer_start_time}s" 2>&1 | grep -c "/api/answer/history/my" || echo "0")
#   fi
#   printf "  日志中记录的请求数: %d\n" "$answer_count"
#   echo

#   # 汇总结果
#   print_title "负载均衡测试结果汇总"
#   echo "┌─────────────────────┬──────────┬─────────────────┬─────────────────────────┐"
#   echo "│ 服务                │ 实例数   │ 成功率          │ 日志分布                │"
#   echo "├─────────────────────┼──────────┼─────────────────┼─────────────────────────┤"
#   printf "│ %-19s │ %-8s │ %3d/%-2d (%3d%%)   │ 实例1:%d, 实例2:%d       │\n" "user-service" "2" "$user_success" "$request_count" "$((user_success * 100 / request_count))" "$user1_count" "$user2_count"
#   printf "│ %-19s │ %-8s │ %3d/%-2d (%3d%%)   │ 实例1:%d, 实例2:%d       │\n" "question-service" "2" "$question_success" "$request_count" "$((question_success * 100 / request_count))" "$question1_count" "$question2_count"
#   printf "│ %-19s │ %-8s │ %3d/%-2d (%3d%%)   │ 实例1:%d               │\n" "answer-service" "1" "$answer_success" "5" "$((answer_success * 100 / 5))" "$answer_count"
#   echo "└─────────────────────┴──────────┴─────────────────┴─────────────────────────┘"
#   echo
  
#   # 验证负载均衡是否生效
#   echo "负载均衡验证:"
#   if [[ $user1_count -gt 0 && $user2_count -gt 0 ]]; then
#     echo "  ✓ user-service: 两个实例均有请求处理，负载均衡生效"
#     echo "    分布比例: 实例1 $(( user1_count * 100 / (user1_count + user2_count) ))% / 实例2 $(( user2_count * 100 / (user1_count + user2_count) ))%"
#   elif [[ $user1_count -gt 0 || $user2_count -gt 0 ]]; then
#     echo "  ⚠ user-service: 仅检测到 1 个实例处理请求，可能负载均衡未生效"
#   else
#     echo "  ? user-service: 未能从日志中获取实例信息"
#     echo "    提示: 请确保 Docker 容器正在运行且日志级别允许记录请求"
#   fi
  
#   if [[ $question1_count -gt 0 && $question2_count -gt 0 ]]; then
#     echo "  ✓ question-service: 两个实例均有请求处理，负载均衡生效"
#     echo "    分布比例: 实例1 $(( question1_count * 100 / (question1_count + question2_count) ))% / 实例2 $(( question2_count * 100 / (question1_count + question2_count) ))%"
#   elif [[ $question1_count -gt 0 || $question2_count -gt 0 ]]; then
#     echo "  ⚠ question-service: 仅检测到 1 个实例处理请求，可能负载均衡未生效"
#   else
#     echo "  ? question-service: 未能从日志中获取实例信息"
#     echo "    提示: 请确保 Docker 容器正在运行且日志级别允许记录请求"
#   fi
#   echo
# }

# 并发负载均衡测试（按端口统计并发命中）
test_concurrent_load_balancing() {
  local token="$1"
  local concurrent_count=${2:-20}

  print_title "并发负载均衡测试（端口统计）"
  echo "发送 ${concurrent_count} 个并发请求到 user-service"

  local tmp_dir="./.tmp_lb_conc_$$"
  mkdir -p "$tmp_dir"
  : > "$tmp_dir/ports_user_conc.txt"

  for i in $(seq 1 $concurrent_count); do
    (
      local hdr_file body_file inst resp_code
      hdr_file="$tmp_dir/h_user_c_$i.txt"
      body_file="$tmp_dir/b_user_c_$i.json"
      curl -sS -X GET "${BASE_URL}/api/user/info" \
        -H "Authorization: Bearer ${token}" \
        -H "Content-Type: application/json" \
        -D "$hdr_file" -o "$body_file" 2>/dev/null
      inst=$(grep -i '^X-Instance-Host:' "$hdr_file" | awk -F ': ' '{print $2}' | tr -d '\r')
      if [[ -z "$inst" ]]; then
        inst=$(grep -i '^X-Instance-Id:' "$hdr_file" | awk -F ': ' '{print $2}' | awk -F ':' '{print $1}' | tr -d '\r')
      fi
      if [[ -z "$inst" ]]; then
        inst=$(grep -i '^X-Instance-Port:' "$hdr_file" | awk -F ': ' '{print $2}' | tr -d '\r')
      fi
      [[ -n "$inst" ]] && echo "$inst" >> "$tmp_dir/ports_user_conc.txt"
    ) &
  done

  wait

  echo "  并发实例分布（IP 或标识）："
  if [[ -s "$tmp_dir/ports_user_conc.txt" ]]; then
    awk '{c[$0]++} END{for (k in c) printf "    - 实例 %s: %d 次\n", k, c[k]}' "$tmp_dir/ports_user_conc.txt"
  else
    echo "    未能提取到实例头部（X-Instance-Host / X-Instance-Id / X-Instance-Port）。"
  fi

  rm -rf "$tmp_dir"
  echo
}

# ====================== 中间件使用检测 ======================
# Redis 使用情况检测
test_redis_usage() {
  local token="$1"
  print_title "检查 Redis 使用情况"

  # 通过容器内的 redis-cli 获取统计信息
  if ! docker ps --format '{{.Names}}' | grep -q '^redis$'; then
    echo "未检测到 redis 容器，跳过 Redis 检测。"
    return 0
  fi

  local info_before stats_before keys_before
  info_before=$(docker exec redis redis-cli INFO stats 2>/dev/null || true)
  stats_before=$(echo "$info_before" | sed -n 's/^total_commands_processed:\([0-9]\+\).*/\1/p')
  local hits_before misses_before
  hits_before=$(echo "$info_before" | sed -n 's/^keyspace_hits:\([0-9]\+\).*/\1/p')
  misses_before=$(echo "$info_before" | sed -n 's/^keyspace_misses:\([0-9]\+\).*/\1/p')
  keys_before=$(docker exec redis redis-cli INFO keyspace 2>/dev/null | sed -n 's/^db0:keys=\([0-9]\+\).*/\1/p')
  : "${stats_before:=0}"; : "${hits_before:=0}"; : "${misses_before:=0}"; : "${keys_before:=0}"

  echo "基线：commands=$stats_before, hits=$hits_before, misses=$misses_before, keys=$keys_before"

  # 触发若干可能使用缓存/会话的请求
  for i in $(seq 1 10); do
    curl -sS -X GET "${BASE_URL}/api/quiz/questions" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json" >/dev/null 2>&1 || true
  done
  for i in $(seq 1 10); do
    curl -sS -X GET "${BASE_URL}/api/user/info" \
      -H "Authorization: Bearer ${token}" \
      -H "Content-Type: application/json" >/dev/null 2>&1 || true
  done

  sleep 1

  local info_after stats_after hits_after misses_after keys_after
  info_after=$(docker exec redis redis-cli INFO stats 2>/dev/null || true)
  stats_after=$(echo "$info_after" | sed -n 's/^total_commands_processed:\([0-9]\+\).*/\1/p')
  hits_after=$(echo "$info_after" | sed -n 's/^keyspace_hits:\([0-9]\+\).*/\1/p')
  misses_after=$(echo "$info_after" | sed -n 's/^keyspace_misses:\([0-9]\+\).*/\1/p')
  keys_after=$(docker exec redis redis-cli INFO keyspace 2>/dev/null | sed -n 's/^db0:keys=\([0-9]\+\).*/\1/p')
  : "${stats_after:=0}"; : "${hits_after:=0}"; : "${misses_after:=0}"; : "${keys_after:=0}"

  local d_cmd=$((stats_after - stats_before))
  local d_hits=$((hits_after - hits_before))
  local d_miss=$((misses_after - misses_before))
  local d_keys=$((keys_after - keys_before))

  echo "变化：commands+=$d_cmd, hits+=$d_hits, misses+=$d_miss, keys+=$d_keys"

  if [[ $d_cmd -gt 0 ]]; then
    echo "✓ Redis 已被访问（命令计数增长）"
  else
    echo "⚠ 未观察到 Redis 命令增长，可能未命中缓存或会话。" >&2
    FAIL_COUNT=$((FAIL_COUNT+1))
  fi
}

# RabbitMQ 使用情况检测（依赖管理端口 15672）
test_rabbitmq_usage() {
  local token="$1"
  print_title "检查 RabbitMQ 使用情况"

  local RMQ_API="http://localhost:15672/api/overview"
  local auth="guest:guest"
  local overview_before overview_after

  if ! curl -sS -u "$auth" "$RMQ_API" >/dev/null 2>&1; then
    echo "无法访问 RabbitMQ 管理接口，跳过 RabbitMQ 检测。"
    return 0
  fi

  # 队列/交换机更精确统计（避免 overview 的聚合为 0）
  local Q_STATS="answer.stats.queue"
  local Q_SUBMIT="answer.submit.queue"

  # 读取队列基线
  local q_stats_before q_submit_before
  q_stats_before=$(curl -sS -u "$auth" "http://localhost:15672/api/queues/%2F/${Q_STATS}" || echo '{}')
  q_submit_before=$(curl -sS -u "$auth" "http://localhost:15672/api/queues/%2F/${Q_SUBMIT}" || echo '{}')

  # 提取基线中的计数
  local stats_msgs_b=0 stats_deliver_b=0 submit_msgs_b=0 submit_deliver_b=0
  if has_jq; then
    stats_msgs_b=$(echo "$q_stats_before" | jq -r '.messages // 0')
    stats_deliver_b=$(echo "$q_stats_before" | jq -r '.message_stats.deliver_get // (.message_stats.deliver // 0) // 0')
    submit_msgs_b=$(echo "$q_submit_before" | jq -r '.messages // 0')
    submit_deliver_b=$(echo "$q_submit_before" | jq -r '.message_stats.deliver_get // (.message_stats.deliver // 0) // 0')
  else
    stats_msgs_b=$(printf '%s' "$q_stats_before" | sed -n 's/.*"messages"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p')
    submit_msgs_b=$(printf '%s' "$q_submit_before" | sed -n 's/.*"messages"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p')
    [[ -z "$stats_msgs_b" ]] && stats_msgs_b=0
    [[ -z "$submit_msgs_b" ]] && submit_msgs_b=0
    stats_deliver_b=$(printf '%s' "$q_stats_before" | sed -n 's/.*"deliver_get"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p')
    submit_deliver_b=$(printf '%s' "$q_submit_before" | sed -n 's/.*"deliver_get"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p')
    [[ -z "$stats_deliver_b" ]] && stats_deliver_b=0
    [[ -z "$submit_deliver_b" ]] && submit_deliver_b=0
  fi
  echo "基线：${Q_STATS}.messages=$stats_msgs_b, deliver_get=$stats_deliver_b; ${Q_SUBMIT}.messages=$submit_msgs_b, deliver_get=$submit_deliver_b"

  # 获取题目并多次提交答案以触发消息
  local q_resp qid
  q_resp=$(curl_json GET "/api/quiz/questions" "$token")
  if has_jq; then
    qid=$(echo "$q_resp" | jq -r '.data[0].questionId // 2')
  else
    qid=$(printf '%s' "$q_resp" | sed -n 's/.*"questionId"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p' | head -1)
    [[ -z "$qid" ]] && qid=2
  fi

  local i body
  body=$(cat <<EOF
{"questionId": ${qid}, "selectedOption": 1}
EOF
)
  for i in $(seq 1 5); do
    curl_json POST "/api/answer/submit" "$token" "$body" >/dev/null 2>&1 || true
    sleep 0.15
  done

  sleep 1

  # 读取队列最新
  local q_stats_after q_submit_after
  q_stats_after=$(curl -sS -u "$auth" "http://localhost:15672/api/queues/%2F/${Q_STATS}" || echo '{}')
  q_submit_after=$(curl -sS -u "$auth" "http://localhost:15672/api/queues/%2F/${Q_SUBMIT}" || echo '{}')

  local stats_msgs_a=0 stats_deliver_a=0 submit_msgs_a=0 submit_deliver_a=0
  if has_jq; then
    stats_msgs_a=$(echo "$q_stats_after" | jq -r '.messages // 0')
    stats_deliver_a=$(echo "$q_stats_after" | jq -r '.message_stats.deliver_get // (.message_stats.deliver // 0) // 0')
    submit_msgs_a=$(echo "$q_submit_after" | jq -r '.messages // 0')
    submit_deliver_a=$(echo "$q_submit_after" | jq -r '.message_stats.deliver_get // (.message_stats.deliver // 0) // 0')
  else
    stats_msgs_a=$(printf '%s' "$q_stats_after" | sed -n 's/.*"messages"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p')
    submit_msgs_a=$(printf '%s' "$q_submit_after" | sed -n 's/.*"messages"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p')
    [[ -z "$stats_msgs_a" ]] && stats_msgs_a=0
    [[ -z "$submit_msgs_a" ]] && submit_msgs_a=0
    stats_deliver_a=$(printf '%s' "$q_stats_after" | sed -n 's/.*"deliver_get"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p')
    submit_deliver_a=$(printf '%s' "$q_submit_after" | sed -n 's/.*"deliver_get"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p')
    [[ -z "$stats_deliver_a" ]] && stats_deliver_a=0
    [[ -z "$submit_deliver_a" ]] && submit_deliver_a=0
  fi

  local d_stats_msgs=$((stats_msgs_a - stats_msgs_b))
  local d_stats_del=$((stats_deliver_a - stats_deliver_b))
  local d_submit_msgs=$((submit_msgs_a - submit_msgs_b))
  local d_submit_del=$((submit_deliver_a - submit_deliver_b))

  echo "变化：${Q_STATS}.messages+=$d_stats_msgs, deliver_get+=$d_stats_del; ${Q_SUBMIT}.messages+=$d_submit_msgs, deliver_get+=$d_submit_del"

  if [[ $d_submit_msgs -gt 0 || $d_stats_msgs -gt 0 || $d_submit_del -gt 0 || $d_stats_del -gt 0 ]]; then
    echo "✓ RabbitMQ 已被使用（队列计数/投递增长）"
  else
    echo "⚠ 未观察到 RabbitMQ 队列计数增长，可能业务未触发或消费者未运行。" >&2
    FAIL_COUNT=$((FAIL_COUNT+1))
  fi
}

# Elasticsearch 日志入库检测
test_elasticsearch_logs() {
  print_title "检查 Elasticsearch 日志入库"
  local ES="http://localhost:9200"
  if ! curl -sS "$ES/_cluster/health" >/dev/null 2>&1; then
    echo "无法访问 Elasticsearch，跳过 ES 检测。"
    return 0
  fi

  local counts
  counts=$(curl -sS "$ES/_cat/indices/logstash-*?h=docs.count" 2>/dev/null || true)
  if [[ -z "$counts" ]]; then
    echo "未找到 logstash-* 索引；日志可能尚未写入。" >&2
    FAIL_COUNT=$((FAIL_COUNT+1))
    return 0
  fi

  local total=0 n
  while read -r n; do
    [[ -z "$n" ]] && continue
    total=$((total + n))
  done <<< "$counts"

  echo "logstash-* 文档总数: $total"
  if [[ $total -gt 0 ]]; then
    echo "✓ 检测到日志写入 Elasticsearch"
  else
    echo "⚠ 未检测到 Elasticsearch 日志文档。" >&2
    FAIL_COUNT=$((FAIL_COUNT+1))
  fi
}

# Prometheus 目标检测
test_prometheus_targets() {
  print_title "检查 Prometheus 采集目标"
  local PROM="http://localhost:9090"
  if ! curl -sS "$PROM/api/v1/targets" >/dev/null 2>&1; then
    echo "无法访问 Prometheus，跳过 Prometheus 检测。"
    return 0
  fi

  local targets_json up_count=0 total=0
  targets_json=$(curl -sS "$PROM/api/v1/targets" || echo '{}')
  if has_jq; then
    total=$(echo "$targets_json" | jq '.data.activeTargets | length')
    up_count=$(echo "$targets_json" | jq '[.data.activeTargets[] | select(.health=="up")] | length')
  else
    total=$(printf '%s' "$targets_json" | sed -n 's/.*activeTargets":[[:space:]]*\[\(.*\)\].*/\1/p' | wc -c)
    up_count=$(printf '%s' "$targets_json" | grep -o '"health":"up"' | wc -l)
  fi
  : "${total:=0}"; : "${up_count:=0}"
  echo "目标数: $total, UP: $up_count"
  if [[ $total -gt 0 && $up_count -gt 0 ]]; then
    echo "✓ Prometheus 正在采集目标"
  else
    echo "⚠ 未检测到 Prometheus 有效目标。" >&2
    FAIL_COUNT=$((FAIL_COUNT+1))
  fi
}

# Zipkin 追踪检测
test_zipkin_traces() {
  print_title "检查 Zipkin 追踪"
  local ZK="http://localhost:9411"
  if ! curl -sS "$ZK/api/v2/services" >/dev/null 2>&1; then
    echo "无法访问 Zipkin，跳过追踪检测。"
    return 0
  fi
  local services
  services=$(curl -sS "$ZK/api/v2/services" || echo '[]')
  echo "服务列表: $services"
  local hit=0
  for s in gateway-service user-service question-service answer-service; do
    if echo "$services" | grep -q "$s"; then hit=1; fi
  done
  if [[ $hit -eq 1 ]]; then
    echo "✓ 检测到 Zipkin 服务名称，链路已上报"
  else
    echo "⚠ 未检测到微服务出现在 Zipkin 服务列表。" >&2
    FAIL_COUNT=$((FAIL_COUNT+1))
  fi
}

# Nacos 注册检测（可选）
test_nacos_registry() {
  print_title "检查 Nacos 注册信息"
  local NC="http://localhost:8848/nacos/v1/ns/catalog/services?pageNo=1&pageSize=100&namespaceId=dev"
  if ! curl -sS "$NC" >/dev/null 2>&1; then
    echo "无法访问 Nacos，跳过注册检测。"
    return 0
  fi
  local resp count=0
  resp=$(curl -sS "$NC" || echo '{}')
  if has_jq; then
    count=$(echo "$resp" | jq -r '.count // 0')
  else
    count=$(printf '%s' "$resp" | sed -n 's/.*"count"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p')
    [[ -z "$count" ]] && count=0
  fi
  echo "注册服务数量: $count"
  if [[ $count -gt 0 ]]; then
    echo "✓ Nacos 已存在注册服务"
  else
    echo "⚠ 未检测到 Nacos 注册服务。" >&2
    FAIL_COUNT=$((FAIL_COUNT+1))
  fi

  # 解析并汇总各服务实例状态（优先使用 jq）
  if has_jq; then
    echo "服务实例概览:"
    echo "$resp" | jq -r '.serviceList[] | "\(.name),\(.groupName),\(.ipCount),\(.healthyInstanceCount)"' |
      while IFS="," read -r name group ip healthy; do
        printf "  - %-16s 组=%-12s IP=%-2s 健康=%-2s\n" "$name" "$group" "$ip" "$healthy"
      done

    # 校验关键服务的期望实例数
    local exp_ok=1
    check_expect() {
      local svc="$1" exp="$2" actual
      actual=$(echo "$resp" | jq -r ".serviceList[] | select(.name==\"$svc\") | .healthyInstanceCount // -1")
      if [[ "$actual" -eq "$exp" ]]; then
        echo "✓ $svc 实例数符合预期 ($exp)"
      else
        echo "⚠ $svc 预期 $exp，实际 $actual" >&2
        exp_ok=0
      fi
    }
    check_expect "user-service" 2
    check_expect "question-service" 2
    check_expect "gateway-service" 1
    check_expect "answer-service" 1
    # seata-server 作为外部组件，不进行严格校验

    if [[ $exp_ok -eq 0 ]]; then
      FAIL_COUNT=$((FAIL_COUNT+1))
    fi
  else
    echo "提示: 未安装 jq，仅输出服务总数。安装 jq 可获得更详细的实例统计。"
  fi
}

main_flow() {
  wait_gateway_up

  # 自动登录用户，获取最新 USER_TOKEN
  print_title "用户登录以获取令牌"
  if [[ -z "$USER_TOKEN" ]]; then
    USER_TOKEN=$(login_and_get_token "$USER_NAME" "$USER_PASSWORD") || true
  fi
  if [[ -n "$USER_TOKEN" ]]; then
    echo "已获取用户令牌"
  else
    echo "未获取到用户令牌，后续请求可能失败。"
  fi

  # ========== 负载均衡测试 ==========
  if [[ -n "$USER_TOKEN" ]]; then
    test_load_balancing "$USER_TOKEN" 10
    test_concurrent_load_balancing "$USER_TOKEN" 20
    # ========== 中间件使用检测 ==========
    test_redis_usage "$USER_TOKEN"
    # test_rabbitmq_usage "$USER_TOKEN"
    # test_elasticsearch_logs
    test_prometheus_targets
    test_zipkin_traces
    test_nacos_registry
  fi

  # 1) 获取当前用户信息（USER_TOKEN）
  print_title "获取当前用户信息"
  resp=$(curl_json GET "/api/user/info" "$USER_TOKEN")
  echo "$resp" | (has_jq && jq . || cat)
  assert_ok_code "$resp"

  # 2) 获取题目列表
  print_title "获取题目列表"
  resp=$(curl_json GET "/api/quiz/questions" "$USER_TOKEN")
  echo "$resp" | (has_jq && jq . || cat)
  assert_ok_code "$resp"

  local first_qid=2
  if has_jq; then
    first_qid=$(echo "$resp" | jq -r '.data[0].questionId // 2')
  else
    first_qid=$(printf '%s' "$resp" | sed -n 's/.*"questionId"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p' | head -1)
    [[ -z "$first_qid" ]] && first_qid=2
  fi
  echo "选用题目ID: ${first_qid}"

  # 3) 提交答案（USER_TOKEN）
  print_title "提交答案"
  submit_body=$(cat <<EOF
{"questionId": ${first_qid}, "selectedOption": 1}
EOF
)
  resp=$(curl_json POST "/api/answer/submit" "$USER_TOKEN" "$submit_body")
  echo "$resp" | (has_jq && jq . || cat)
  assert_ok_code "$resp"

  local history_id=""
  if has_jq; then
    history_id=$(echo "$resp" | jq -r '.data.answerHistoryId // empty')
  else
    history_id=$(printf '%s' "$resp" | sed -n 's/.*"answerHistoryId"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p')
  fi
  echo "提交生成记录ID: ${history_id}"

  # 4) 获取当前用户答题记录（USER_TOKEN）
  print_title "获取当前用户答题记录"
  resp=$(curl_json GET "/api/answer/history/my" "$USER_TOKEN")
  echo "$resp" | (has_jq && jq . || cat)
  assert_ok_code "$resp"

  # 若未获 ID，则从记录列表取一个
  if [[ -z "$history_id" ]]; then
    if has_jq; then
      history_id=$(echo "$resp" | jq -r '.data[0].answerHistoryId // empty')
    else
      history_id=$(printf '%s' "$resp" | sed -n 's/.*"answerHistoryId"[[:space:]]*:[[:space:]]*\([0-9]\+\).*/\1/p' | head -1)
    fi
  fi

  if [[ -n "$history_id" ]]; then
    # 5) 通过 id 获取记录
    print_title "通过ID获取记录"
    resp=$(curl_json GET "/api/answer/history/${history_id}" "$USER_TOKEN")
    echo "$resp" | (has_jq && jq . || cat)
    assert_ok_code "$resp"

    # 管理员令牌：若未提供尝试登录，否则跳过管理员步骤
    if [[ -z "$ADMIN_TOKEN" ]]; then
      print_title "管理员登录（可选）"
      ADMIN_TOKEN=$(login_and_get_token "$ADMIN_USER" "$ADMIN_PASSWORD") || true
      if [[ -n "$ADMIN_TOKEN" ]]; then
        echo "已获取管理员令牌"
      else
        echo "未获取到管理员令牌，跳过管理员相关测试。"
      fi
    fi

    if [[ -n "$ADMIN_TOKEN" ]]; then
      # 6) 删除记录（ADMIN_TOKEN，若权限要求）
      if [[ -n "$history_id" ]]; then
        print_title "删除记录 (admin)"
        resp=$(curl_json DELETE "/api/answer/history/${history_id}" "$ADMIN_TOKEN")
        echo "$resp" | (has_jq && jq . || cat)
        assert_ok_code "$resp"
      fi

      # 7) 获取所有记录（ADMIN_TOKEN）
      print_title "获取所有答题记录 (admin)"
      resp=$(curl_json GET "/api/answer/history/all" "$ADMIN_TOKEN")
      echo "$resp" | (has_jq && jq . || cat)
      assert_ok_code "$resp"
    fi
  else
    echo "提示：未能解析到答题记录ID，跳过按ID测试。"
  fi

  echo
  if [[ "$FAIL_COUNT" -eq 0 ]]; then
    echo "✅ 全部测试通过"
  else
    echo "❌ 测试存在失败，失败数：$FAIL_COUNT"
    exit 1
  fi
}

main_flow
