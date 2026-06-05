update system_setting
set setting_key = case setting_key
    when ('myagent' || '.openai.default-model') then 'agent.studio.openai.default-model'
    when ('myagent' || '.runtime.default-agent-timeout-seconds') then 'agent.studio.runtime.default-agent-timeout-seconds'
    when ('myagent' || '.runtime.default-llm-timeout-seconds') then 'agent.studio.runtime.default-llm-timeout-seconds'
    when ('myagent' || '.runtime.default-java-method-timeout-seconds') then 'agent.studio.runtime.default-java-method-timeout-seconds'
    when ('myagent' || '.runtime.default-external-agent-timeout-seconds') then 'agent.studio.runtime.default-external-agent-timeout-seconds'
    when ('myagent' || '.runtime.default-max-steps') then 'agent.studio.runtime.default-max-steps'
    when ('myagent' || '.runtime.default-max-agent-call-depth') then 'agent.studio.runtime.default-max-agent-call-depth'
    else setting_key
end
where setting_key in (
    ('myagent' || '.openai.default-model'),
    ('myagent' || '.runtime.default-agent-timeout-seconds'),
    ('myagent' || '.runtime.default-llm-timeout-seconds'),
    ('myagent' || '.runtime.default-java-method-timeout-seconds'),
    ('myagent' || '.runtime.default-external-agent-timeout-seconds'),
    ('myagent' || '.runtime.default-max-steps'),
    ('myagent' || '.runtime.default-max-agent-call-depth')
);
