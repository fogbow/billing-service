package cloud.fogbow.fs.core.datastore;

import org.springframework.data.jpa.repository.JpaRepository;

import cloud.fogbow.fs.core.models.FinanceUser;
import cloud.fogbow.fs.core.models.UserId;

public interface UserRepository extends JpaRepository<FinanceUser, String>{
    FinanceUser findByUserId(UserId userId);
}
