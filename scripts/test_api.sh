#!/usr/bin/env bash
set -uo pipefail

# Cloud Quiz API 集成测试脚本
# 依赖: curl, jq (可选但推荐)
# 用法: chmod +x scripts/test_api.sh && ./scripts/test_api.sh

BASE_URL=${BASE_URL:-"http://localhost:8090"}
# 若未显式提供，将自动登录获取最新 token
USER_NAME=${USER_NAME:-"test"}
USER_PASSWORD=${USER_PASSWORD:-"123456"}
ADMIN_USER=${ADMIN_USER:-"admin"}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-"123456"}
USER_TOKEN=${USER_TOKEN:-""}
ADMIN_TOKEN=${ADMIN_TOKEN:-""}
FAIL_COUNT=0

has_jq() { command -v jq >/dev/null 2>&1; }

print_title() {
  echo
  echo "==== $1 ===="
}

extract_json_field() {
  # 轻量 JSON 提取（在无 jq 环境下使用），仅适用于简单键值
  # 用法：extract_json_field "$json" token
  local json="$1" key="$2"
  # 尝试匹配 "key":"value" 形式
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
    # 无法解析 code，尝试基于已知错误关键字判定
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
  # 日志输出到 stderr，避免污染返回值
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
