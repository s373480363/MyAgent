insert into schema_definition(
  schema_key,
  version,
  name,
  description,
  json_schema,
  java_type,
  created_from,
  status,
  locked
)
select
  'java.sample.echo.input',
  1,
  '示例 JavaMethod 输入',
  'V1 内置示例 JavaMethod 输入契约，用于验证输入透传和运行链路。',
  '{
    "type": "object",
    "additionalProperties": true
  }'::jsonb,
  'com.fasterxml.jackson.databind.JsonNode',
  'JAVA_METHOD_SCAN',
  'ACTIVE',
  true
where not exists (
  select 1
  from schema_definition
  where schema_key = 'java.sample.echo.input'
    and version = 1
);

insert into schema_definition(
  schema_key,
  version,
  name,
  description,
  json_schema,
  java_type,
  created_from,
  status,
  locked
)
select
  'java.sample.echo.output',
  1,
  '示例 JavaMethod 输出',
  'V1 内置示例 JavaMethod 输出契约，用于验证输出透传和运行链路。',
  '{
    "type": "object",
    "additionalProperties": true
  }'::jsonb,
  'com.fasterxml.jackson.databind.JsonNode',
  'JAVA_METHOD_SCAN',
  'ACTIVE',
  true
where not exists (
  select 1
  from schema_definition
  where schema_key = 'java.sample.echo.output'
    and version = 1
);

insert into schema_definition(
  schema_key,
  version,
  name,
  description,
  json_schema,
  java_type,
  created_from,
  status,
  locked
)
select
  'tool.sample.echo.input',
  1,
  '示例 Tool 输入',
  'V1 内置示例 Tool 输入契约，用于验证 TOOL 节点输入透传和 Trace 链路。',
  '{
    "type": "object",
    "additionalProperties": true
  }'::jsonb,
  'com.fasterxml.jackson.databind.JsonNode',
  'TOOL_DEFINITION',
  'ACTIVE',
  true
where not exists (
  select 1
  from schema_definition
  where schema_key = 'tool.sample.echo.input'
    and version = 1
);

insert into schema_definition(
  schema_key,
  version,
  name,
  description,
  json_schema,
  java_type,
  created_from,
  status,
  locked
)
select
  'tool.sample.echo.output',
  1,
  '示例 Tool 输出',
  'V1 内置示例 Tool 输出契约，用于验证 TOOL 节点输出透传和 Trace 链路。',
  '{
    "type": "object",
    "additionalProperties": true
  }'::jsonb,
  'com.fasterxml.jackson.databind.JsonNode',
  'TOOL_DEFINITION',
  'ACTIVE',
  true
where not exists (
  select 1
  from schema_definition
  where schema_key = 'tool.sample.echo.output'
    and version = 1
);

insert into java_method_definition(
  method_key,
  name,
  description,
  bean_name,
  method_name,
  input_schema_id,
  output_schema_id,
  status
)
select
  'java.sample.echo',
  '示例 JavaMethod 回显',
  'V1 内置示例 JavaMethod，可用于验收输入透传、Trace 和 Schema 链路。',
  'systemEchoJavaMethod',
  'execute',
  input_schema.id,
  output_schema.id,
  'ENABLED'
from schema_definition input_schema
join schema_definition output_schema
  on output_schema.schema_key = 'java.sample.echo.output'
 and output_schema.version = 1
where input_schema.schema_key = 'java.sample.echo.input'
  and input_schema.version = 1
  and not exists (
    select 1
    from java_method_definition
    where method_key = 'java.sample.echo'
  );

insert into tool_definition(
  tool_key,
  name,
  description,
  input_schema_id,
  output_schema_id,
  executor_type,
  executor_config_json,
  status
)
select
  'tool.sample.echo',
  '示例 Tool 回显',
  'V1 内置示例 Tool，可用于验收输入透传、Trace 和 Schema 链路。',
  input_schema.id,
  output_schema.id,
  'ECHO',
  '{}'::jsonb,
  'ENABLED'
from schema_definition input_schema
join schema_definition output_schema
  on output_schema.schema_key = 'tool.sample.echo.output'
 and output_schema.version = 1
where input_schema.schema_key = 'tool.sample.echo.input'
  and input_schema.version = 1
  and not exists (
    select 1
    from tool_definition
    where tool_key = 'tool.sample.echo'
  );
