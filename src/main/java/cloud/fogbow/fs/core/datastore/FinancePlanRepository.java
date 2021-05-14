package cloud.fogbow.fs.core.datastore;

import org.springframework.data.jpa.repository.JpaRepository;

import cloud.fogbow.fs.core.models.FinancePlan;

public interface FinancePlanRepository extends JpaRepository<FinancePlan, String>{

}
