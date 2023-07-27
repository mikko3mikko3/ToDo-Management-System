package com.dmm.task.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.dmm.task.data.entity.Tasks;
import com.dmm.task.data.repository.TasksRepository;
import com.dmm.task.form.TaskForm;
import com.dmm.task.service.AccountUserDetails;

@Controller
public class TaskController {

	@Autowired
	private TasksRepository repo;

	@GetMapping("/main")
	public String getCalendar(Model model) {

		// 1. 2次元表になるので、ListのListを用意する
		// 2. 1週間分のLocalDateを格納するListを用意する
		List<LocalDate> week = new ArrayList<>();
		List<List<LocalDate>> matrix = new ArrayList<>();

		// 3. その月の1日のLocalDateを取得する
		LocalDate firstday = LocalDate.now().withDayOfMonth(1);

		// 4.
		// 曜日を表すDayOfWeekを取得し、上で取得したLocalDateに曜日の値（DayOfWeek#getValue)をマイナスして前月分のLocalDateを求める
		DayOfWeek w = firstday.getDayOfWeek();
		LocalDate day = firstday.minusDays(w.getValue());

		// 5. 1日ずつ増やしてLocalDateを求めていき、2．で作成したListへ格納していき、1週間分詰めたら1．のリストへ格納する
		for (int i = 1; i <= 7; i++) {
			week.add(day);
			day = day.plusDays(1);
		}
		matrix.add(week);
		week = new ArrayList<>();

		// 6.
		// 2週目以降は単純に1日ずつ日を増やしながらLocalDateを求めてListへ格納していき、土曜日になったら1．のリストへ格納して新しいListを生成する
		int length = LocalDate.now().lengthOfMonth(); // 今月の長さ
		
		for (int i = day.getDayOfMonth(); i <= length; i++) {
			DayOfWeek w2 =day.getDayOfWeek();
			week.add(day);
			if (w2 == DayOfWeek.SATURDAY) {
				matrix.add(week);
				week = new ArrayList<>();
			}
			day = day.plusDays(1);
		}

		// 7. 最終週の翌月分をDayOfWeekの値を使って計算し、6．で生成したリストへ格納し、最後に1．で生成したリストへ格納する
		w = day.getDayOfWeek();
		for (int i = 1; i <= 7 - w.getValue(); i++) {
			week.add(day);
			day = day.plusDays(1);
		}
		matrix.add(week);

		MultiValueMap<LocalDate, Tasks> tasks = new LinkedMultiValueMap<LocalDate, Tasks>();
		model.addAttribute("matrix", matrix);
		model.addAttribute("tasks", tasks);

		return "/main";
	}

	/**
	 * 投稿を作成.
	 * 
	 * @param taskForm 送信データ
	 * @param user     ユーザー情報
	 * @return 遷移先
	 */
	@PostMapping("/main/create")
	public String create(@Validated TaskForm taskForm, BindingResult bindingResult,
			@AuthenticationPrincipal AccountUserDetails user, Model model) {
		// バリデーションの結果、エラーがあるかどうかチェック
		if (bindingResult.hasErrors()) {
			// エラーがある場合は投稿登録画面を返す
			List<Tasks> list = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
			model.addAttribute("tasks", list);
			model.addAttribute("taskForm", taskForm);
			return "/main";
		}

		Tasks task = new Tasks();
		task.setName(user.getName());
		task.setTitle(taskForm.getTitle());
		task.setText(taskForm.getText());
		task.setDate(taskForm.getDate().atTime(0,0));

		repo.save(task);

		return "redirect:/main";
	}

	/**
	 * 投稿を編集.
	 * 
	 * @param taskForm 送信データ
	 * @return 遷移先
	 */
	@PostMapping("/main/edit/{id}")
	public String edit(@Validated TaskForm taskForm, BindingResult bindingResult,
			@AuthenticationPrincipal AccountUserDetails user, Model model ) {
		// バリデーションの結果、エラーがあるかどうかチェック
				if (bindingResult.hasErrors()) {
					// エラーがある場合は編集画面を返す
					Tasks task = repo.findById(taskForm.getId()).get();
					model.addAttribute("taskForm", taskForm);
					model.addAttribute("task", task);
					return "edit";
				}
				return null;
			}

	/**
	 * タスクの削除.
	 * 
	 * @param id タスクID
	 * @return 遷移先
	 */
	@PostMapping("/main/delete/{id}")
	public String delete(@PathVariable Integer id) {
		repo.deleteById(id);
		return  "redirect:/main";
	}

	/**
	 * タスクの新規作成画面.
	 * 
	 * @param model モデル
	 * @param date  追加対象日
	 * @return タスクの新規作成画面のンプレート名
	 */
	@GetMapping("/main/create/{date}")
	public String create(@DateTimeFormat(pattern = "yyyy-MM-dd") @PathVariable LocalDate date, Model model) {

		return "create";
	}

}