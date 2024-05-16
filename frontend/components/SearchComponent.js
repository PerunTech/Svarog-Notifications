import { React, connect, PropTypes, axios, elements, GenericGrid, ComponentManager } from 'perun-core'
const { alertUser } = elements
import { iconManager } from './svgHolder'
import { getMainLabel } from '../utils/labels'
import { jsonToURI } from '../utils/utils'


const tableName = 'SUBJECT'

class SearchComponent extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      title: '',
      text: '',
      category: '',
      priority: ''
    };
  }

  componentDidMount() {
    this.getCategoryDropdown();
    this.getPriorityDropdown();
  }

  getCategoryDropdown = () => {
    const { svSession } = this.props
    const url = window.server + '/ReactElements/getTableWithFilter/' + svSession + '/' + 'SVAROG_CODES/' + 'PARENT_CODE_VALUE/' + 'CATEGORY/' + '10000' + '/DESC'
    axios.get(url).then(res => {
      if (res.data) {
        const categories = {}
        res.data.forEach(category => {
          console.log(category)
          Reflect.set(categories, category['SVAROG_CODES.CODE_VALUE'], category['SVAROG_CODES.LABEL_CODE'])
        })
        this.setState({ categories })
      }
    }).catch((error) => {
      console.error(error);
      alertUser(true, 'error', error.response?.data?.title || error, error.response?.data?.message || '');
    })
  }


  getPriorityDropdown = () => {
    const { svSession } = this.props
    const url = window.server + '/ReactElements/getTableWithFilter/' + svSession + '/' + 'SVAROG_CODES/' + 'PARENT_CODE_VALUE/' + 'PRIORITY/' + '10000' + '/ASC'
    axios.get(url).then(res => {
      if (res.data) {
        const priorities = {}
        res.data.forEach(priority => {
          console.log(priority)
          Reflect.set(priorities, priority['SVAROG_CODES.CODE_VALUE'], priority['SVAROG_CODES.LABEL_CODE'])
        })
        this.setState({ priorities })
      }
    }).catch((error) => {
      console.error(error);
      alertUser(true, 'error', error.response?.data?.title || error, error.response?.data?.message || '');
    })
  }

  handleSearch = () => {
    const { svSession } = this.props
    const { title, text, category, priority } = this.state
    const dataObj = { SUBJECT_TITLE: title, MSG_TEXT: text, SUBJECT_CATEGORY: category, SUBJECT_PRIORITY: priority }
    const data = jsonToURI(dataObj)
    const url = `${window.server}/SvarogNotificationsServices/searchSubjects/${svSession}`
    const reqConfig = { method: 'post', data, url, headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }

    if (title === '' && text === '' && category === '' || category === '0' && priority === '' || priority === '0') {
      alertUser(true, 'error', 'Please enter at least one search value', null)
    } else {
      axios(reqConfig).then(res => {
        if (res && res.data) {
          const gridId = `${tableName}_SEARCH_GRID`
          const gridConfig = `/SvarogNotificationsServices/getTableFieldList/${svSession}/${tableName}`
          const grid = <GenericGrid
            key={gridId}
            id={gridId}
            gridType='SEARCH_GRID_DATA'
            configTableName={gridConfig}
            dataTableName={res.data}
            // onRowClickFunct={this.onAssetRowClick} 
            minHeight={600}
            floatDownloadBtnsToRight
            customClassName={'customGridClass'}
          />
          ComponentManager.cleanComponentReducerState(gridId)
          this.setState({ grid: undefined }, () => this.setState({ grid }))
        }

      }).catch((error) => {
        console.error(error);
        alertUser(true, 'error', error.response?.data?.title || error, error.response?.data?.message || '');
      })
    }
  }

  generateSearchForm = () => {
    const { title, text, category, priority, categories, priorities } = this.state
    return <div id='search-form' className={'form'}>
      <div id='search-values' className={'values-container-secondary'}>
        <div id='title-search-input' className='mr-2'>
          <label htmlFor='title' className={'control-label input-label'}>
            {getMainLabel('title', this.context)}
          </label>
          <input type='text' name='title' id='title' className='form-control' style={{ borderTopRightRadius: '0', borderBottomRightRadius: '0' }}
            value={title} onChange={this.onChange} onKeyDown={this.handleSearchByTheEnterKey}
          />
        </div>
        <div id='text-search-input' className='mr-2'>
          <label htmlFor='text' className={'control-label input-label'}>
            {getMainLabel('text', this.context)}
          </label>
          <input type='text' name='text' id='text' className='form-control' style={{ borderTopRightRadius: '0', borderBottomRightRadius: '0' }}
            value={text} onChange={this.onChange} onKeyDown={this.handleSearchByTheEnterKey}
          />
        </div>
        <div id='category-search-input' className='mr-3'>
          <label htmlFor='category' className={'control-label input-label'}>
            {getMainLabel('category', this.context)}
          </label>
          <select id='category' name='category' className='form-control no-border-radius'
            onChange={this.onChange} onKeyDown={this.handleSearchByTheEnterKey} value={category}
          ><option selected value='0'>Select a category</option>
            {categories && Object.keys(categories).map((key, index) => (
              <option key={key} value={key}>{Object.values(categories)[index]}</option>
            ))}
          </select>
        </div>
        <div id='priority-search-input' className='mr-0'>
          <label htmlFor='priority' className={'control-label input-label'}>
            {getMainLabel('priority', this.context)}
          </label>
          <select id='priority' name='priority' className='form-control no-border-radius'
            onChange={this.onChange} onKeyDown={this.handleSearchByTheEnterKey} value={priority}
          ><option selected value='0'>Select a priority</option>
            {priorities && Object.keys(priorities).map((key, index) => (
              <option key={key} value={key}>{Object.values(priorities)[index]}</option>
            ))}
          </select>
        </div>

        <div id='search-button-container'>
          <button id='search' className={'button-secondary no-border-radius no-border'} onClick={this.handleSearch}>{iconManager.getIcon('search')}Search</button>
        </div>
      </div>
    </div>
  }


  handleSearchByTheEnterKey = e => {
    if (e.keyCode === 13) {
      this.handleSearch()
    }
  }

  onChange = e => {
    this.setState({ [e.target.name]: e.target.value })
  }


  render() {
    const { grid } = this.state

    return <div id='container' className='height-90-percent'>
      <div className={'searchGridHolder'}>
        <div className='col-md-12 d-flex flex-row align-items-center mt-2 mb-2'>{this.generateSearchForm()}</div>
      </div>
      <div className='col-md-12 mt-n2 mb-2' style={{ marginBottom: '2vh' }}>{grid}</div>
    </div>
  }
}

SearchComponent.contextTypes = {
  intl: PropTypes.object.isRequired
}

const mapStateToProps = state => ({
  svSession: state.security.svSession
})

SearchComponent.contextTypes = {
  intl: PropTypes.object.isRequired
}

export default connect(mapStateToProps)(SearchComponent)