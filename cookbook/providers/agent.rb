action :create do
  node.set[:baragon][:agent_yaml]['loadBalancerConfig']['name'] = new_resource.group

  # rubocop:disable Metrics/LineLength
  ["#{node['baragon']['agent_yaml']['loadBalancerConfig']['rootPath']}/proxy",
   "#{node['baragon']['agent_yaml']['loadBalancerConfig']['rootPath']}/upstreams"].each do |dir|
    directory dir do
      recursive true
    end
  end
  # rubocop:enable Metrics/LineLength

  baragon_agent_jar = "BaragonAgentService-#{node[:baragon][:version]}.jar"

  remote_file "/usr/share/java/#{baragon_agent_jar}" do
    action :create
    backup 5
    owner 'root'
    group 'root'
    mode 0644
    source "file://#{Chef::Config[:file_cache_path]}/Baragon/" \
           "BaragonAgentService/target/#{baragon_agent_jar}"
  end

  node.set[:baragon][:agent_yaml][:zookeeper][:quorum] =
    node[:baragon][:zk_hosts].join(',')
  node.set[:baragon][:agent_yaml][:zookeeper][:zkNamespace] =
    node[:baragon][:zk_namespace]

  if node[:nginx]
    unless node[:nginx][:binary]
      fail "attribute :binary not found in node[:nginx]: #{node[:nginx].inspect}"
    end
    node.set['baragon']['agent_yaml']['loadBalancerConfig']['checkConfigCommand'] =
      "#{node[:nginx][:binary]} -t"
    node.set['baragon']['agent_yaml']['loadBalancerConfig']['reloadConfigCommand'] =
      "#{node[:nginx][:binary]} -s reload"
  else
    node.set['baragon']['agent_yaml']['loadBalancerConfig']['checkConfigCommand'] =
    '/bin/true'
    node.set['baragon']['agent_yaml']['loadBalancerConfig']['reloadConfigCommand'] =
    '/bin/true'
  end

  file "/etc/baragon/agent-#{new_resource.group}.yml" do
    action :create
    owner 'root'
    group 'root'
    mode 0644
    content(YAML.dump(JSON.parse(node[:baragon][:agent_yaml].merge(
          templates:
            [node[:baragon][:proxy_template],
             node[:baragon][:upstream_template]]).to_hash.to_json)
      )
    )
    notifies :restart, "service[baragon-agent-#{new_resource.group}]"
  end

  template "/etc/init/baragon-agent-#{new_resource.group}.conf" do
    source 'baragon-agent.init.erb'
    cookbook 'baragon'
    owner 'root'
    group 'root'
    mode 0644
    notifies :restart, "service[baragon-agent-#{new_resource.group}]"
    variables baragon_jar: baragon_agent_jar,
              config_yaml: "/etc/baragon/agent-#{new_resource.group}.yml"
  end

  service "baragon-agent-#{new_resource.group}" do
    provider Chef::Provider::Service::Upstart
    supports status: true,
             restart: true
    action [:enable, :start]
  end
end

action :delete do
  fail 'Not Implemented'
end
