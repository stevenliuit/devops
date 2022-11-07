package io.onedev.server.model.support.administration.jobexecutor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.Size;

import io.onedev.server.buildspec.Service;
import io.onedev.server.util.match.Matcher;
import io.onedev.server.util.match.PathMatcher;
import io.onedev.server.util.patternset.PatternSet;
import io.onedev.server.web.editable.annotation.Editable;
import io.onedev.server.web.editable.annotation.Patterns;

@Editable
public class ServiceLocator implements Serializable {

	private static final long serialVersionUID = 1L;

	private String serviceNames;
	
	private String serviceImages;
	
	private List<NodeSelectorEntry> nodeSelector = new ArrayList<>();
	
	@Editable(order=100, name="适用名称", placeholder="所有", description=""
			+ "可以选择指定适用于此定位器的以空格分隔的服务名称。 使用“*”或“？” 用于通配符匹配。 前缀 '-' 排除。 留空以匹配所有")
	@Patterns
	public String getServiceNames() {
		return serviceNames;
	}

	public void setServiceNames(String serviceNames) {
		this.serviceNames = serviceNames;
	}
	
	@Editable(order=200, name="Applicable Images", placeholder="所有", description=""
			+ "可以选择指定适用于此定位器的以空格分隔的服务图像。 使用“**”、“*”或“？” 用于<a href='$docRoot/pages/path-wildcard.md' target='_blank'>路径通配符匹配</a>。 前缀 '-' 排除。 留空以匹配所有")
	@Patterns(path=true)
	public String getServiceImages() {
		return serviceImages;
	}

	public void setServiceImages(String serviceImages) {
		this.serviceImages = serviceImages;
	}

	@Editable(order=300, description="指定此定位器的节点选择器")
	@Size(min=1, message="至少应指定一个条目")
	public List<NodeSelectorEntry> getNodeSelector() {
		return nodeSelector;
	}

	public void setNodeSelector(List<NodeSelectorEntry> nodeSelector) {
		this.nodeSelector = nodeSelector;
	}
	
	public final boolean isApplicable(Service service) {
		Matcher matcher = new PathMatcher();
		return (getServiceNames() == null || PatternSet.parse(getServiceNames()).matches(matcher, service.getName()))
				&& (getServiceImages() == null || PatternSet.parse(getServiceImages()).matches(matcher, service.getImage()));
	}
	
}