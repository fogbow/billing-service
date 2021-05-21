package cloud.fogbow.fs.core.datastore;

import org.springframework.data.jpa.repository.JpaRepository;

import cloud.fogbow.fs.core.plugins.PlanPlugin;

public interface PlanPluginRepository extends JpaRepository<PlanPlugin, String>{
}
